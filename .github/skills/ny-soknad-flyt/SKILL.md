---
name: ny-soknad-flyt
description: |
  Arkitektur og mønstre for ny søknad-flyt (orkestrator) vs gammel quiz-flyt i dp-soknad-orkestrator.
  Bruk denne skillen når du jobber med søknad-livssyklus, tilstander, metrikker, Kafka-meldinger
  eller REST-endepunkter for søknader — spesielt under og etter utrulling av ny søknad-flyt.
---

# Ny søknad-flyt vs gammel quiz-flyt

## Oversikt

Det finnes to søknad-flyter i dp-soknad-orkestrator som eksisterer parallelt under utrullingen
av ny søknad. Begge flytene bruker samme `soknad`-tabell i databasen.

| | Gammel flyt (quiz) | Ny flyt (orkestrator) |
|---|---|---|
| **Opprinnelse** | Kafka-event fra dp-soknad | REST API fra frontend |
| **Trigger** | `søknad_innsendt_varsel` | `POST /soknad` |
| **Mottak** | `SøknadMottak` | `SøknadApi` + `SøknadService.opprett()` |
| **Opplysninger** | `quiz_opplysning`-tabeller | `seksjon_v2`-tabell |
| **Innsending** | Allerede innsendt ved mottak | `POST /soknad/{id}` → `sendInn()` |
| **Journalføring** | Separat journalføring-flyt | Via PDF-behov-kjede |

---

## Gammel quiz-flyt

### Livssyklus

```
dp-soknad (quiz-app)
  → Kafka: søknad_innsendt_varsel
    → SøknadMottak.onPacket()
      → søknadRepository.lagreQuizSøknad(søknadmelding.tilSøknad())
        → SøknadMapper → dekomponerer søknadData
        → skriver til SøknadTabell + quiz_opplysning
      → søknadService.publiserMeldingOmSøknadInnsendt()
      → søknadService.opprettOgLagreKomplettSøknaddata()
```

Sletting av gammel søknad skjer via Kafka:
```
dp-soknad → Kafka: søknad_slettet
  → SøknadSlettetMottak → søknadService.slettSøknadOgInkrementerMetrikk(..., kilde="quiz")
```

### Nøkkelfiler

- `søknad/mottak/SøknadMottak.kt` — Kafka-lytter for `søknad_innsendt_varsel`
- `søknad/SøknadMapper.kt` — Mapper quiz-data til `Søknad` + opplysninger
- `søknad/mottak/SøknadSlettetMottak.kt` — Kafka-lytter for `søknad_slettet`
- `quizOpplysning/` — Alle datatyper og repository for quiz-opplysninger

---

## Ny søknad-flyt (orkestrator)

### Livssyklus

```
Frontend
  → POST /soknad
    → SøknadService.opprett()                  → Tilstand: PÅBEGYNT
    → Kafka: søknad_endret_tilstand (PÅBEGYNT)

  → PUT /seksjon/{søknadId}/{seksjonId}        → SeksjonService lagrer svar
  → POST /soknad/{id}                          → SøknadService.sendInn()
    → Kafka: søknad_klar_til_journalføring
      → MeldingOmSøknadKlarTilJournalføringMottak
        → søknadRepository.markerSøknadSomInnsendt() → Tilstand: INNSENDT
        → Kafka: behov generer_og_mellomlagre_søknad_pdf
          → SøknadPdfGenerertOgMellomlagretMottak
            → Kafka: behov journalfør_søknad_pdf_og_vedlegg
              → SøknadPdfOgVedleggJournalførtMottak
                → søknadRepository.markerSøknadSomJournalført() → Tilstand: JOURNALFØRT
```

Sletting av ny søknad:
- **Bruker-initiert:** `DELETE /soknad/{id}` → `slettSøknadOgInkrementerMetrikk(..., kilde="ny")`
- **Automatisk (7 dager påbegynt):** `slettSøknaderSomErPåbegyntOgIkkeOppdatertPå7Dager()`

### Nøkkelfiler

- `søknad/SøknadApi.kt` — REST-endepunkter: `POST /soknad`, `DELETE /soknad/{id}`, `POST /soknad/{id}`
- `søknad/SøknadService.kt` — Forretningslogikk for opprett, sendInn, slett
- `søknad/seksjon/SeksjonApi.kt` — REST-endepunkter for seksjon-svar under `/seksjon/{søknadId}/{seksjonId}`
- `søknad/seksjon/SeksjonService.kt` — Lagring og henting av seksjoner
- `søknad/seksjon/SeksjonRepository.kt` — DB-tilgang mot `seksjon_v2`-tabellen
- `søknad/melding/MeldingOmSøknadKlarTilJournalføringMottak.kt` — Starter PDF-kjeden
- `søknad/mottak/SøknadPdfGenerertOgMellomlagretMottak.kt` — Mellomsteg i PDF-kjeden
- `søknad/mottak/SøknadPdfOgVedleggJournalførtMottak.kt` — Avslutter journalføring

---

## Tilstander (`Tilstand`-enum)

```kotlin
enum class Tilstand {
    PÅBEGYNT,           // Ny flyt: opprettet men ikke sendt inn
    INNSENDT,           // Begge flyter: innsendt/mottatt
    JOURNALFØRT,        // Begge flyter: journalført
    SLETTET_AV_SYSTEM,  // Automatisk slettet etter 7 dager uten aktivitet
}
```

Gammel flyt starter direkte i `INNSENDT` (quiz-søknaden er allerede innsendt når den ankommer).
Ny flyt starter i `PÅBEGYNT`. Happy path er `PÅBEGYNT → INNSENDT → JOURNALFØRT`.

Ny søknad kan også avsluttes uten å gå hele happy path:

- **Bruker-initiert sletting:** hard delete + `søknad_endret_tilstand` med `nyTilstand = "Slettet"`
- **Automatisk sletting:** `slettSøknadSomSystem()` setter tilstand til `SLETTET_AV_SYSTEM`

---

## Metrikker

Alle metrikker er under namespace `dp_soknad_orkestrator`.

### Delte metrikker (med `kilde`-label)

| Metrikk | `kilde="ny"` | `kilde="quiz"` |
|---|---|---|
| `antall_soknader_mottatt{kilde}` | `SøknadService.sendInn()` | `SøknadMottak.onPacket()` |
| `antall_soknader_slettet{kilde}` | REST DELETE + auto-slett | `SøknadSlettetMottak` |

### Ny flyt-spesifikke metrikker

| Metrikk | Når |
|---|---|
| `antall_soknader_opprettet` | `SøknadService.opprett()` — søknad i PÅBEGYNT |
| `antall_soknader_journalfort` | `SøknadPdfOgVedleggJournalførtMottak` — vellykket journalføring |

### Gammel flyt-spesifikke metrikker

| Metrikk | Når |
|---|---|
| `antall_soknader_varslet` | `publiserMeldingOmSøknadInnsendt()` |
| `antall_soknader_dekomponert` | `SøknadMapper` — vellykket dekomponering |
| `antall_soknader_dekomponering_feilet` | `SøknadMapper` — feilet dekomponering |

### Typisk Grafana-funnel for ny søknad

```
opprettet → mottatt{kilde="ny"} → journalfort
```

Under gradvis utrulling, sammenlign:
```
mottatt{kilde="ny"} vs mottatt{kilde="quiz"}
```

---

## Database

Begge flyter bruker `soknad`-tabellen (`SøknadTabell`). Skillet:

- **Gammel flyt:** Bruker også `quiz_opplysning`-tabellene. `søknad.opplysninger` er ikke-tom.
- **Ny flyt:** Bruker `seksjon_v2`-tabellen. `søknad.opplysninger` er alltid tom.

```kotlin
// Praktisk heuristikk for gammel quiz-søknad:
søknad.opplysninger.isNotEmpty()

// Praktisk heuristikk for ny søknad:
søknad.opplysninger.isEmpty()
```

Dette er en nyttig heuristikk når du allerede har en `Søknad`-instans, men det er ikke et
eksplisitt kildefelt i domenemodellen.

---

## Kafka-meldinger

### Gammel flyt — inn

| Event | Kilde | Håndteres av |
|---|---|---|
| `søknad_innsendt_varsel` | dp-soknad | `SøknadMottak` |
| `søknad_slettet` | dp-soknad | `SøknadSlettetMottak` |

### Ny flyt — ut

| Event | Utgiver | Innhold |
|---|---|---|
| `søknad_endret_tilstand` | `SøknadService` | Tilstandsendring (PÅBEGYNT/INNSENDT/Slettet) |
| `søknad_klar_til_journalføring` | `SøknadService.sendInn()` | Trigger for PDF-kjeden |
| `behov generer_og_mellomlagre_søknad_pdf` | `MeldingOmSøknadKlarTilJournalføringMottak` | PDF-generering |
| `behov journalfør_søknad_pdf_og_vedlegg` | `SøknadPdfGenerertOgMellomlagretMottak` | Journalføring |
| `dokumentkrav_innsendt` | `MeldingOmSøknadKlarTilJournalføringMottak` | Dokumentasjonskrav |

---

## Viktige konvensjoner

- Begge flyter bruker `slettSøknadOgInkrementerMetrikk(søknadId, ident, kilde)` — pass alltid
  riktig `kilde` (`"ny"` eller `"quiz"`) siden dette driver metrikk-labelen.
- Auto-sletting (7 dager) bruker `slettSøknadSomSystem()` + `SøknadMetrikker.slettet.labelValues("ny")` —
  disse søknadene er alltid ny flyt siden gammel flyt aldri er i PÅBEGYNT.
- `hentSøknaderForIdent()` bruker inner join med `seksjon_v2` — derfor vises ikke søknader uten
  rader i `seksjon_v2` i oversikten. Det gjelder både gamle quiz-søknader og nye søknader som er
  opprettet, men som ennå ikke har lagret noen seksjoner.
