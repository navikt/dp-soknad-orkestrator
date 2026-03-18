---
name: barneredigering
description: |
  Barn (child) editing for saksbehandlere in dp-soknad-orkestrator.
  Use this skill when working on barn-related features: editing, adding, or fetching
  children in søknader, the saksbehandler_barn table, barn behovløsere, or the
  dp-behandling integration for barn opplysninger.
---

# Barneredigering — Saksbehandler barn editing

## Architecture overview

Saksbehandlere can view, edit, and add barn (children) for a søknad. The system supports
both the old quiz-søknad and the new seksjon_v2 søknad format through a unified read/write
layer.

### Read priority chain

When fetching barn, these sources are checked in order:

1. **`saksbehandler_barn` table** — Latest snapshot of saksbehandler edits. If present, this
   is the source of truth.
2. **`quiz_opplysning` tables** — Old søknad: register barn (`faktum.register.barn-liste`) +
   egne barn (`faktum.barn-liste`).
3. **`seksjon_v2` table** — New søknad: barnetillegg seksjon JSON with `barnFraPdl` and
   `barnLagtManuelt` arrays.

Once a saksbehandler makes any edit, `saksbehandler_barn` shadows the original source for
that søknad.

### Write flow

Both `oppdaterBarn` and `leggTilBarn`:
1. Read current barn from the priority chain above
2. Apply the edit (update or append)
3. INSERT a full snapshot (all barn) into `saksbehandler_barn`
4. Send to dp-behandling via POST

Writes never go to `quiz_opplysning` — `saksbehandler_barn` is the only write target.

### saksbehandler_barn table (append-only)

Each row stores the **complete barn list** as JSON. No UPDATEs, only INSERTs. Latest row
per søknad = current state. All previous rows = edit history.

```sql
CREATE TABLE saksbehandler_barn (
    id        BIGSERIAL PRIMARY KEY,
    soknad_id UUID NOT NULL REFERENCES soknad(soknad_id),
    barn      JSON NOT NULL,         -- Full List<BarnSvar> as JSON array
    endret_av TEXT NOT NULL,         -- Saksbehandler ID
    opprettet TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
```

Uses `JSON` (not `JSONB`) for consistency with `seksjon_v2` and to preserve key order.

## Key files

### Service layer
- `src/main/kotlin/.../opplysning/OpplysningService.kt` — Core barn CRUD. Contains
  `hentBarn`, `hentAlleBarnSvar` (priority chain), `oppdaterBarn`, `leggTilBarn`,
  `erEndret`, `sendbarnTilDpBehandling`, `hentBarnFraQuizOpplysninger`,
  `hentBarnFraSeksjon`.

### Repositories
- `src/main/kotlin/.../opplysning/SaksbehandlerBarnRepository.kt` — Interface:
  `hentBarn(søknadId): List<BarnSvar>?` and `lagreBarn(søknadId, barn, endretAv)`.
- `src/main/kotlin/.../opplysning/SaksbehandlerBarnRepositoryPostgres.kt` — Exposed ORM
  implementation. Reads latest row by `opprettet DESC`, inserts new snapshot rows.
- `src/main/kotlin/.../opplysning/SaksbehandlerBarnTabell.kt` — Exposed table object.
- `src/main/kotlin/.../quizOpplysning/db/QuizOpplysningRepository.kt` — Old quiz repo
  (still used for reading barn from old søknad via fallback).

### API spec
- `openapi/src/main/resources/soknad-orkestrator-api.yaml` — OpenAPI spec defining all
  request/response schemas. The frontend (dp-saksbehandling-frontend) consumes these
  endpoints. DTOs in `openapi/build/generated/` are auto-generated from this spec.
  - **`BarnRequest`** — Unified request wrapper for both POST and PUT. Contains `barn: BarnData`
    and optional `behandlingId: UUID?`.
  - **`BarnData`** — The barn fields (fornavnOgMellomnavn, etternavn, fodselsdato, etc.).
    No `barnId` — that's in the path for PUT.

### API
- `src/main/kotlin/.../opplysning/OpplysningApi.kt` — Route handlers. Endpoints:
  - `GET /opplysninger/{soknadId}/barn` — Fetch barn by søknadId (deprecated)
  - `PUT /opplysninger/{soknadId}/barn/oppdater` — Deprecated, returns 400
  - `GET /opplysninger/barn/{soknadbarnId}` — Fetch barn by søknadbarnId
  - `PUT /opplysninger/barn/{soknadbarnId}/{barnId}` — Update specific barn
  - `POST /opplysninger/barn/{soknadbarnId}` — Add new barn
- Authentication: Azure AD (`saksbehandler` group required)

### dp-behandling integration
- `src/main/kotlin/.../opplysning/DpBehandlingKlient.kt` — HTTP client.
  `oppdaterBarnOpplysning(behandlingId, dpBehandlingOpplysning, token)`.
  - Endpoint: `POST /behandling/{behandlingId}/opplysning/`
  - Body: `NyOpplysningDTO(opplysningstype: UUID, verdi: String, begrunnelse: String,
    gyldigFraOgMed: LocalDate?, gyldigTilOgMed: LocalDate?)`
  - `verdi` is a JSON-serialized `BarnetilleggV2Løsning`
  - Hardcoded barn opplysningstype: `0194881f-9428-74d5-b160-f63a4c61a23b`
  - Uses OBO token flow via Azure AD

### Behovløsere
- `src/main/kotlin/.../behov/løsere/BarnetilleggV2BehovLøser.kt` — Resolves
  `BarnetilleggV2` behov. Same read priority chain as OpplysningService. Contains the
  internal data classes `BarnetilleggV2Løsning` and `LøsningsbarnV2`.
- `src/main/kotlin/.../behov/løsere/BarnetilleggBehovLøser.kt` — Old V1 resolver.
  Has companion constants `BESKRIVENDE_ID_PDL_BARN` and `BESKRIVENDE_ID_EGNE_BARN`.

### Domain model
- `src/main/kotlin/.../quizOpplysning/datatyper/Barn.kt` — `BarnSvar` data class and
  `Barn.barnetilleggperiode(fødselsdato)` which returns `fødselsdato to fødselsdato + 18 years`.

### Wiring
- `src/main/kotlin/.../ApplicationBuilder.kt` — Creates repositories and wires them into
  `OpplysningService` and `BehovløserFactory`.

## Key data types

### BarnSvar (internal domain model)
```kotlin
data class BarnSvar(
    val barnSvarId: UUID,
    val fornavnOgMellomnavn: String,
    val etternavn: String,
    val fødselsdato: LocalDate,
    val statsborgerskap: String,        // Country code, e.g. "NOR"
    val forsørgerBarnet: Boolean,
    val fraRegister: Boolean,           // true = from PDL, false = manually added
    val kvalifisererTilBarnetillegg: Boolean,
    val barnetilleggFom: LocalDate?,    // = fødselsdato when kvalifiserer=true
    val barnetilleggTom: LocalDate?,    // = fødselsdato + 18 years
    val endretAv: String?,              // Saksbehandler ID
    val begrunnelse: String?,
)
```

### LøsningsbarnV2 (sent to dp-behandling inside BarnetilleggV2Løsning.barn)
```kotlin
internal data class LøsningsbarnV2(
    val fornavnOgMellomnavn: String,
    val etternavn: String,
    val fødselsdato: LocalDate,
    val statsborgerskap: String,
    val kvalifiserer: Boolean,
    val barnetilleggFom: LocalDate?,
    val barnetilleggTom: LocalDate?,
    val endretAv: String?,
    val begrunnelse: String?,
)
```

### BarnetilleggV2Løsning (wrapper with søknadbarnId)
```kotlin
internal data class BarnetilleggV2Løsning(
    val søknadbarnId: UUID?,
    val barn: List<LøsningsbarnV2>,
)
```

## Seksjon v2 barn JSON structure

The barnetillegg seksjon stores barn like this:
```json
{
  "barnFraPdl": [
    {
      "id": "uuid",
      "fornavnOgMellomnavn": "...",
      "etternavn": "...",
      "fødselsdato": "2013-05-26",
      "bostedsland": "NOR",
      "forsørgerDuBarnet": "ja"
    }
  ],
  "barnLagtManuelt": [
    {
      "id": "uuid",
      "fornavnOgMellomnavn": "...",
      "etternavn": "...",
      "fødselsdato": "2025-10-21",
      "bostedsland": "ARG"
    }
  ]
}
```

Note: `barnLagtManuelt` entries may lack `forsørgerDuBarnet`. The `id` field is used as
`barnSvarId`. Field `bostedsland` maps to `statsborgerskap`.

## Testing patterns

### OpplysningServiceTest
Uses mockk for all repositories. Key pattern for saksbehandlerBarnRepository:
```kotlin
private val saksbehandlerBarnRepository = mockk<SaksbehandlerBarnRepository>(relaxed = true).also {
    every { it.hentBarn(any()) } returns null  // Default: no saksbehandler edits
}
```
Override per test when testing saksbehandler_barn priority.

### OpplysningApiTest
Uses `InMemoryQuizOpplysningRepository` for quiz repo + mockk for others. The
saksbehandlerBarnRepository mock captures saves and returns them on subsequent reads:
```kotlin
val saksbehandlerBarnRepository = mockk<SaksbehandlerBarnRepository>(relaxed = true).also { mock ->
    val storedBarn = mutableMapOf<UUID, List<BarnSvar>>()
    every { mock.hentBarn(any()) } answers { storedBarn[firstArg()] }
    every { mock.lagreBarn(any(), any(), any()) } answers { storedBarn[firstArg()] = secondArg() }
}
```

### Auth in API tests
Uses `TestApplication.withMockAuthServerAndTestApplication()` and `testAzureADToken`.
Requires `System.setProperty("Grupper.saksbehandler", "saksbehandler")` in setup.

## Important conventions

- `behandlingId` can be null in `BarnRequestDTO`. When null, skip the dp-behandling call.
- `barnId` is a path parameter for PUT, not in the request body.
- `barnetilleggFom`/`Tom` are set from `Barn.barnetilleggperiode(fødselsdato)` only when
  `kvalifisererTilBarnetillegg == true`. Otherwise null.
- Begrunnelse is per-barn (inside JSON), not per-snapshot.
- The `barn_soknad_mapping` table maps `søknadbarnId ↔ søknadId` for dp-behandling.
- objectMapper config: `FAIL_ON_UNKNOWN_PROPERTIES = false`, dates as ISO strings.
