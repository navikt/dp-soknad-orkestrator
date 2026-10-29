---
name: barneredigering
description: |
  Saksbehandlers redigering av barn i dp-soknad-orkestrator.
  Bruk denne skillen når du jobber med barn i søknader: redigere, legge til, slette eller hente barn,
  tabellen saksbehandler_barn, barn-behovløsere eller integrasjonen mot dp-behandling for barn-opplysninger.
---

# Barneredigering for saksbehandlere

Sist verifisert mot koden: oktober 2026.

## Arkitektur

Saksbehandlere kan se, endre, legge til og slette barn i en søknad. Samme lese- og skrivelag
håndterer både gamle quiz-søknader og nye søknader i `seksjon_v2`.

### Leserekkefølge

Når appen henter barn, sjekker den kildene i denne rekkefølgen:

1. **`saksbehandler_barn`**: siste snapshot av saksbehandlers endringer. Finnes det en rad, er den fasit.
2. **`quiz_opplysning`-tabellene**: gammel søknad. Registerbarn (`faktum.register.barn-liste`) og
   egne barn (`faktum.barn-liste`).
3. **`seksjon_v2`**: ny søknad. Barnetillegg-seksjonen har JSON med listene `barnFraPdl` og
   `barnLagtManuelt`.

Når en saksbehandler har gjort én endring, skygger `saksbehandler_barn` for den opprinnelige kilden.

### Skriveflyt

`oppdaterBarn`, `leggTilBarn` og `slettBarn` følger samme mønster:

1. Les nåværende barn etter leserekkefølgen over
2. Gjør endringen (oppdater, legg til eller fjern)
3. INSERT et fullt snapshot (alle barn) i `saksbehandler_barn`
4. Send til dp-behandling med POST

Appen skriver aldri til `quiz_opplysning`. `saksbehandler_barn` er eneste skrivemål.

### Tabellen saksbehandler_barn (append-only)

Hver rad lagrer **hele barnelista** som JSON. Bare INSERT, aldri UPDATE. Siste rad per søknad er
gjeldende tilstand, og tidligere rader er endringshistorikken.

```sql
CREATE TABLE saksbehandler_barn (
    id        BIGSERIAL PRIMARY KEY,
    soknad_id UUID NOT NULL REFERENCES soknad(soknad_id),
    barn      JSON NOT NULL,         -- Hele List<BarnSvar> som JSON-array
    endret_av TEXT NOT NULL,         -- Saksbehandler-ID
    opprettet TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
```

Migrasjonen ligger i `src/main/resources/db/migration/V22__CREATE_TABLE_SAKSBEHANDLER_BARN.sql`.
Kolonnen bruker `JSON` (ikke `JSONB`) for å være lik `seksjon_v2` og bevare rekkefølgen på nøklene.

## Nøkkelfiler

### Service-laget
- `src/main/kotlin/.../opplysning/OpplysningService.kt`: kjernelogikken for barn. Inneholder
  `hentBarn`, `hentAlleBarnSvar` (leserekkefølgen), `erEndret`, `oppdaterBarn`, `leggTilBarn`,
  `slettBarn`, `sendbarnTilDpBehandling`, `mapTilSøknadId`, `tidligsteBarnetilleggFom`,
  `hentBarnFraQuizOpplysninger` og `hentBarnFraSeksjon`.

### Repositories
- `src/main/kotlin/.../opplysning/SaksbehandlerBarnRepository.kt`: interface med
  `hentBarn(søknadId): List<BarnSvar>?` og `lagreBarn(søknadId, barn, endretAv)`.
- `src/main/kotlin/.../opplysning/SaksbehandlerBarnRepositoryPostgres.kt`: Exposed-implementasjon.
  Leser siste rad sortert på `opprettet DESC` og setter inn nye snapshot-rader.
- `src/main/kotlin/.../opplysning/SaksbehandlerBarnTabell.kt`: Exposed-tabellobjektet.
- `src/main/kotlin/.../quizOpplysning/db/QuizOpplysningRepository.kt`: quiz-repoet. Brukes fortsatt
  til å lese barn fra gamle søknader og til `mapTilSøknadId`.
- `src/main/kotlin/.../quizOpplysning/db/Tabeller.kt`: `BarnSøknadMappingTabell`
  (`barn_soknad_mapping`), som kobler `søknadbarnId` til `søknadId`.

### API-spesifikasjon
- `openapi/src/main/resources/soknad-orkestrator-api.yaml`: OpenAPI-spesifikasjonen for alle
  request- og response-skjemaer. Frontend (dp-saksbehandling-frontend) bruker disse endepunktene.
  DTO-ene i `openapi/build/generated/` genereres fra spesifikasjonen.
  - **`BarnRequest`**: felles request-wrapper for POST og PUT. Har `barn: BarnData`
    og valgfri `behandlingId: UUID?`.
  - **`BarnData`**: feltene for barnet (fornavnOgMellomnavn, etternavn, fodselsdato og så videre).
    Har ikke `barnId`, den ligger i stien for PUT.
  - **`SlettBarnRequest`**: request-body for sletting. Har `behandlingId: UUID` (påkrevd)
    og `begrunnelse: String` (påkrevd).

### API
- `src/main/kotlin/.../opplysning/OpplysningApi.kt`: route-handlere. Endepunkter:
  - `GET /opplysninger/{soknadId}/barn`: henter barn med søknadId (deprecated)
  - `PUT /opplysninger/{soknadId}/barn/oppdater`: deprecated, returnerer 400
  - `GET /opplysninger/barn/{soknadbarnId}`: henter barn med søknadbarnId
  - `PUT /opplysninger/barn/{soknadbarnId}/{barnId}`: oppdaterer ett barn
  - `POST /opplysninger/barn/{soknadbarnId}`: legger til et barn
  - `POST /opplysninger/barn/{soknadbarnId}/{barnId}/slett`: sletter ett barn
- Autentisering: Azure AD, krever gruppen `saksbehandler`
- PUT returnerer uten endring hvis `erEndret` er `false`.

### Integrasjon mot dp-behandling
- `src/main/kotlin/.../opplysning/DpBehandlingKlient.kt`: HTTP-klient med
  `oppdaterBarnOpplysning(behandlingId, dpBehandlingOpplysning, token)`.
  - Endepunkt: `POST /behandling/{behandlingId}/opplysning/`
  - Body: `NyOpplysningDTO(opplysningstype: UUID, verdi: String, begrunnelse: String,
    gyldigFraOgMed: LocalDate?, gyldigTilOgMed: LocalDate?)`
  - `verdi` er en JSON-serialisert `BarnetilleggV2Løsning`
  - `gyldigFraOgMed` settes til `tidligsteBarnetilleggFom(alleBarn)`: minste
    `barnetilleggFom` blant barn med `kvalifisererTilBarnetillegg == true`. `null` hvis
    ingen barn kvalifiserer.
  - `gyldigTilOgMed` er alltid `null` (sendes ikke)
  - Hardkodet opplysningstype for barn: `0194881f-9428-74d5-b160-f63a4c61a23b` (`BARN_OPPLYSNINGSTYPE_ID`)
  - Bruker OBO-token via Azure AD

### Behovløsere
- `src/main/kotlin/.../behov/løsere/BarnetilleggV2BehovLøser.kt`: løser behovet
  `BarnetilleggV2`. Samme leserekkefølge som `OpplysningService`. Inneholder de interne
  dataklassene `BarnetilleggV2Løsning` og `LøsningsbarnV2`.
- `src/main/kotlin/.../behov/løsere/BarnetilleggBehovLøser.kt`: gammel V1-løser.
  Har konstantene `BESKRIVENDE_ID_PDL_BARN` og `BESKRIVENDE_ID_EGNE_BARN` i companion object.

### Domenemodell
- `src/main/kotlin/.../quizOpplysning/datatyper/Barn.kt`: dataklassen `BarnSvar` og
  `Barn.barnetilleggperiode(fødselsdato)`, som returnerer `fødselsdato to fødselsdato + 18 år`.

### Metrikk
- `OpplysningMetrikker.endringBarn` (`dp_soknad_orkestrator_antall_endringer_barn`) økes i
  `OpplysningApi` når et barn oppdateres (bare hvis `erEndret`) eller slettes. Den økes ikke når
  et barn legges til.

### Wiring
- `src/main/kotlin/.../ApplicationBuilder.kt`: lager repositoriene og sender dem inn i
  `OpplysningService` og `BehovløserFactory`.

## Datatyper

### BarnSvar (intern domenemodell)
```kotlin
data class BarnSvar(
    val barnSvarId: UUID,
    val fornavnOgMellomnavn: String,
    val etternavn: String,
    val fødselsdato: LocalDate,
    val statsborgerskap: String,        // Landkode, f.eks. "NOR"
    val forsørgerBarnet: Boolean,
    val fraRegister: Boolean,           // true = fra PDL, false = lagt til manuelt
    val kvalifisererTilBarnetillegg: Boolean,
    val barnetilleggFom: LocalDate?,    // = fødselsdato når kvalifiserer = true
    val barnetilleggTom: LocalDate?,    // = fødselsdato + 18 år
    val endretAv: String?,              // Saksbehandler-ID
    val begrunnelse: String?,
)
```

### LøsningsbarnV2 (sendes til dp-behandling i BarnetilleggV2Løsning.barn)
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

### BarnetilleggV2Løsning (wrapper med søknadbarnId)
```kotlin
internal data class BarnetilleggV2Løsning(
    val søknadbarnId: UUID?,
    val barn: List<LøsningsbarnV2>,
)
```

## Barn i seksjon_v2

Barnetillegg-seksjonen lagrer barn slik:
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

Elementer i `barnLagtManuelt` kan mangle `forsørgerDuBarnet`. Feltet `id` brukes som
`barnSvarId`, og `bostedsland` mappes til `statsborgerskap`.

## Testmønstre

### OpplysningServiceTest
Bruker mockk for alle repositoriene. Standardoppsett for `saksbehandlerBarnRepository`:
```kotlin
private val saksbehandlerBarnRepository = mockk<SaksbehandlerBarnRepository>(relaxed = true).also {
    every { it.hentBarn(any()) } returns null  // Standard: ingen endringer fra saksbehandler
}
```
Overstyr per test når du tester at `saksbehandler_barn` har forrang.

### OpplysningApiTest
Bruker `InMemoryQuizOpplysningRepository` for quiz-repoet og mockk for resten. Mocken for
`saksbehandlerBarnRepository` tar vare på lagrede barn og returnerer dem ved neste lesing:
```kotlin
val saksbehandlerBarnRepository = mockk<SaksbehandlerBarnRepository>(relaxed = true).also { mock ->
    val storedBarn = mutableMapOf<UUID, List<BarnSvar>>()
    every { mock.hentBarn(any()) } answers { storedBarn[firstArg()] }
    every { mock.lagreBarn(any(), any(), any()) } answers { storedBarn[firstArg()] = secondArg() }
}
```

### Auth i API-tester
Bruker `TestApplication.withMockAuthServerAndTestApplication()` og `testAzureADToken`.
Krever `System.setProperty("Grupper.saksbehandler", "saksbehandler")` i oppsettet.

## Viktige konvensjoner

- `behandlingId` kan være null i `BarnRequestDTO`. Da hopper appen over kallet til dp-behandling.
- `behandlingId` er påkrevd i `SlettBarnRequestDTO`. Sletting uten synk mot dp-behandling støttes ikke.
- `barnId` er en path-parameter for PUT og slett, ikke en del av request-body.
- `barnetilleggFom` og `barnetilleggTom` settes fra `Barn.barnetilleggperiode(fødselsdato)` bare når
  `kvalifisererTilBarnetillegg == true`. Ellers er de null.
- Begrunnelsen ligger per barn (inne i JSON), ikke per snapshot.
- `barn_soknad_mapping` kobler `søknadbarnId` til `søknadId` for dp-behandling.
- objectMapper-oppsett: `FAIL_ON_UNKNOWN_PROPERTIES = false`, datoer som ISO-strenger.
- `gyldigFraOgMed` som sendes til dp-behandling er alltid **minste** `barnetilleggFom` blant barn som
  kvalifiserer, ikke fom for barnet som ble endret. `gyldigTilOgMed` sendes aldri.
