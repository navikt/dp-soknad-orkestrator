---
name: ny-soknad-flyt
description: |
  Arkitektur og mønstre for ny søknad-flyt (orkestrator) og den gamle quiz-flyten i dp-soknad-orkestrator.
  Bruk denne skillen når du jobber med søknad-livssyklus, tilstander, vedtaksstatus, dokumentasjonskrav,
  ettersending, metrikker, Kafka-meldinger eller REST-endepunkter for søknader.
---

# Ny søknad-flyt og gammel quiz-flyt

Sist verifisert mot koden: oktober 2026.

## Oversikt

Appen tar bare imot nye søknader via REST fra frontend (ny flyt). Mottaket av quiz-søknader
(`SøknadMottak` og `SøknadMapper`) ble fjernet i #431. Quiz-data finnes fortsatt i databasen for
historiske søknader, og behovløsere og barneredigering leser dem som fallback.

Begge flytene bruker `soknad`-tabellen.

| | Gammel flyt (quiz) | Ny flyt (orkestrator) |
|---|---|---|
| **Nye søknader** | Tas ikke lenger imot | `POST /soknad` fra frontend |
| **Opplysninger** | `quiz_opplysning`-tabeller (historiske data) | `seksjon_v2`-tabellen |
| **Innsending** | – | `POST /soknad/{søknadId}` → `SøknadService.sendInn()` |
| **Sletting** | Kafka-mottaket finnes, men service-laget sletter bare `PÅBEGYNT` | `DELETE /soknad/{søknadId}` for `PÅBEGYNT`, og automatisk etter 7 dager uten aktivitet |
| **Journalføring** | – | PDF-behovkjeden (se under) |

---

## Gammel quiz-flyt

Quiz-data leses fortsatt av behovløsere og barneredigering. Mottaket for slettemeldinger fra
dp-soknad finnes også:

```
dp-soknad → Kafka: søknad_slettet
  → SøknadSlettetMottak → søknadService.slettSøknadOgInkrementerMetrikk(..., kilde = "quiz")
```

Service-metoden sletter bare søknader i `PÅBEGYNT`. Historiske quiz-søknader i `INNSENDT`
slettes derfor ikke gjennom denne veien. Et mottatt event betyr ikke at søknaden er slettet.

### Nøkkelfiler

- `søknad/mottak/SøknadSlettetMottak.kt`: Kafka-lytter for `søknad_slettet`
- `quizOpplysning/`: datatyper og repository for quiz-opplysninger

---

## Ny søknad-flyt (orkestrator)

### Livssyklus

```
Frontend
  → POST /soknad
    → SøknadService.opprett()                  → Tilstand: PÅBEGYNT
    → Kafka: søknad_endret_tilstand (PÅBEGYNT)

  → PUT /seksjon/{søknadId}/{seksjonId}        → SeksjonService.lagre()
    → Kafka: paabegynt_soknad_seksjon          (statistikk, SeksjonDataTilStatistikk)

  → POST /soknad/{søknadId}                    → SøknadService.sendInn()
    → Kafka: søknad_klar_til_journalføring
      → MeldingOmSøknadKlarTilJournalføringMottak
        → søknadRepository.markerSøknadSomInnsendt() → Tilstand: INNSENDT
        → Kafka: søknad_endret_tilstand (INNSENDT)
        → Kafka: dokumentkrav_innsendt
        → Kafka: behov generer_og_mellomlagre_søknad_pdf
          → SøknadPdfGenerertOgMellomlagretMottak
            → Kafka: behov journalfør_søknad_pdf_og_vedlegg
              → SøknadPdfOgVedleggJournalførtMottak
                → søknadRepository.markerSøknadSomJournalført() → Tilstand: JOURNALFØRT

dp-behandling
  → Kafka: søknadsbehandling_ferdig
    → SøknadsbehandlingFerdigMottak → søknadStatusRepository.lagre()  (tabellen soknad_status)
```

Sletting av ny søknad:

- **Brukeren sletter:** `DELETE /soknad/{søknadId}` → `slettSøknadOgInkrementerMetrikk(..., kilde = "ny")`.
  Service-metoden sletter bare søknader i `PÅBEGYNT`.
- **Automatisk etter 7 dager i PÅBEGYNT:** `SlettSøknaderSomErPåbegyntOgIkkeOppdatertPå7DagerJobb`
  kaller `SøknadService.slettSøknaderSomErPåbegyntOgIkkeOppdatertPå7Dager()`

### Ettersending av dokumentasjon

Etter innsending kan brukeren ettersende dokumentasjon:

```
Frontend
  → PUT /seksjon/{søknadId}/{seksjonId}/dokumentasjonskrav/ettersending
    → SeksjonService.lagreDokumentasjonskravEttersending()
      (krever tilstand INNSENDT eller JOURNALFØRT)
      → Kafka: behov journalfør_ettersending_av_dokumentasjon  (type ETTERSENDING_TIL_DIALOG)
        → MeldingOmEttersendingMottak (på løsningen)
          → seksjonRepository.lagreDokumentasjonskravEttersending()
          → søknadRepository.lagreEttersendingJournalpostId()
          → Kafka: dokumentkrav_innsendt
```

### Vedtaksstatus

`SøknadsbehandlingFerdigMottak` lytter på `søknadsbehandling_ferdig` fra dp-behandling og lagrer
resultatet i `soknad_status` via `SøknadStatusRepository`. Feltet `førteTil` mappes til
`Status`-enumen: `Innvilgelse`, `Avslag`, `Stans`, `Gjenopptak`, `Endring` eller `Ukjent`.
Ukjente verdier lagres som `Ukjent`. Meldingen ignoreres hvis identen ikke eier søknaden.

`GET /soknad/mine-soknader` returnerer siste status som `søknadVedtak` i `SøknadForIdent`.

### REST-endepunkter

`søknad/SøknadApi.kt`:

| Metode og sti | Formål |
|---|---|
| `POST /soknad` | Oppretter søknad |
| `GET /soknad/mine-soknader` | Søknader for innlogget bruker, med tittel, manglende dokumentasjonskrav og `søknadVedtak` |
| `POST /soknad/{søknadId}` | Sender inn søknaden |
| `DELETE /soknad/{søknadId}` | Sletter søknaden |
| `GET /soknad/{søknadId}/progress` | Seksjons-ID-er som er lagret |
| `GET /soknad/{søknadId}/dokumentasjonskrav` | Dokumentasjonskrav for søknaden |
| `PUT /soknad/{søknadId}/personalia` | Lagrer personalia |
| `GET /soknad/{søknadId}/sistoppdatert` | Sist oppdatert-tidspunkt |

`søknad/seksjon/SeksjonApi.kt`:

| Metode og sti | Formål |
|---|---|
| `PUT /seksjon/{søknadId}/{seksjonId}` | Lagrer seksjonssvar, dokumentasjonskrav og PDF-grunnlag |
| `GET /seksjon/{søknadId}/{seksjonId}` | Henter én seksjon |
| `PUT /seksjon/{søknadId}/{seksjonId}/dokumentasjonskrav` | Lagrer dokumentasjonskrav før innsending |
| `PUT /seksjon/{søknadId}/{seksjonId}/dokumentasjonskrav/ettersending` | Ettersender dokumentasjon |
| `GET /seksjon/{søknadId}` | Henter alle seksjoner |

### Nøkkelfiler

- `søknad/SøknadApi.kt`: REST-endepunkter under `/soknad`
- `søknad/SøknadService.kt`: opprett, sendInn, slett, mine søknader, skjemakode
- `søknad/seksjon/SeksjonApi.kt`: REST-endepunkter under `/seksjon`
- `søknad/seksjon/SeksjonService.kt`: lagring av seksjoner, dokumentasjonskrav og ettersending
- `søknad/seksjon/SeksjonRepository.kt`: DB-tilgang mot `seksjon_v2`
- `søknad/melding/MeldingOmSøknadKlarTilJournalføringMottak.kt`: starter PDF-kjeden
- `søknad/mottak/SøknadPdfGenerertOgMellomlagretMottak.kt`: mellomsteg i PDF-kjeden
- `søknad/mottak/SøknadPdfOgVedleggJournalførtMottak.kt`: avslutter journalføringen
- `søknad/mottak/MeldingOmEttersendingMottak.kt`: tar imot løsningen på ettersending
- `søknad/mottak/SøknadsbehandlingFerdigMottak.kt`: lagrer vedtaksstatus
- `søknad/db/SøknadStatusRepository.kt`: DB-tilgang mot `soknad_status`
- `søknad/jobb/SlettSøknaderSomErPåbegyntOgIkkeOppdatertPå7DagerJobb.kt`: automatisk sletting
- `søknad/jobb/RekjørJournalføringForSøknaderJobb.kt`: engangsjobb med hardkodet liste over
  søknad-ID-er som skal journalføres på nytt. Startes fra `ApplicationBuilder`.

---

## Tilstander (`Tilstand`-enum)

```kotlin
enum class Tilstand {
    PÅBEGYNT,           // Opprettet, ikke sendt inn
    INNSENDT,           // Sendt inn
    JOURNALFØRT,        // Journalført
    SLETTET_AV_SYSTEM,  // Slettet automatisk etter 7 dager uten aktivitet
}
```

Gamle quiz-søknader startet i `INNSENDT`. Ny flyt starter i `PÅBEGYNT`, og happy path er
`PÅBEGYNT → INNSENDT → JOURNALFØRT`.

Ny søknad kan også avsluttes uten å gå hele happy path:

- **Brukeren sletter en PÅBEGYNT søknad:** hard delete og `søknad_endret_tilstand` med `nyTilstand = "Slettet"`
- **Automatisk sletting:** `søknadRepository.slettSøknadSomSystem()` setter tilstanden til `SLETTET_AV_SYSTEM`

Vedtaksstatus (`Status`) er ikke en tilstand. Den ligger i `soknad_status`.

---

## Metrikker

Alle metrikker ligger i `metrikker/Metrikker.kt` med namespace `dp_soknad_orkestrator`.

### Søknad (`SøknadMetrikker`)

| Metrikk | Økes i |
|---|---|
| `antall_soknader_opprettet` | `SøknadService.opprett()` |
| `antall_soknader_mottatt{kilde="ny"}` | `SøknadService.sendInn()` |
| `antall_soknader_slettet{kilde}` | Ved sletting, ikke ved avvist sletting. `kilde="ny"`: REST DELETE og automatisk sletting. `kilde="quiz"`: via `SøknadSlettetMottak`, bare for `PÅBEGYNT` |
| `antall_soknader_journalfort` | `SøknadPdfOgVedleggJournalførtMottak` |

### Behov og barn

| Metrikk | Økes i |
|---|---|
| `antall_behov_mottatt{behov}` (`BehovMetrikker.mottatt`) | `BehovMottak` |
| `antall_behov_lost{behov}` (`BehovMetrikker.løst`) | `Behovløser.publiserLøsning()` |
| `antall_endringer_barn` (`OpplysningMetrikker.endringBarn`) | `OpplysningApi`, ved endring av barn |

### Metrikker uten aktive kallere

Disse er definert i `SøknadMetrikker`, men ingen kode øker dem etter at quiz-mottaket ble fjernet i #431.
Avklar med teamet om de fortsatt brukes i Grafana før du sletter dem eller bygger på dem.

- `antall_soknader_varslet`: økes bare i `SøknadService.publiserMeldingOmSøknadInnsendt()`, som ikke har kallere
- `antall_soknader_dekomponert` og `antall_soknader_dekomponering_feilet`: ble økt i `SøknadMapper`, som er slettet

### Typisk Grafana-funnel for ny søknad

```
opprettet → mottatt{kilde="ny"} → journalfort
```

---

## Database

| Tabell | Innhold |
|---|---|
| `soknad` (`SøknadTabell`) | Søknaden, tilstand og tidspunkter. Delt av begge flyter |
| `seksjon_v2` (`SeksjonV2Tabell`) | Seksjonssvar, dokumentasjonskrav og PDF-grunnlag for ny flyt |
| `soknad_status` (`SøknadStatusTabell`) | Vedtaksstatus fra `søknadsbehandling_ferdig` |
| `quiz_opplysning`-tabellene | Historiske quiz-opplysninger |

Slik skiller du flytene når du har en `Søknad`-instans:

```kotlin
// Gammel quiz-søknad
søknad.opplysninger.isNotEmpty()

// Ny søknad
søknad.opplysninger.isEmpty()
```

Dette er en heuristikk. Domenemodellen har ikke et eget kildefelt.

---

## Kafka-meldinger

### Inn

| Event / behov | Kilde | Håndteres av |
|---|---|---|
| `søknad_slettet` | dp-soknad | `SøknadSlettetMottak` |
| `søknad_klar_til_journalføring` | denne appen | `MeldingOmSøknadKlarTilJournalføringMottak` |
| løsning på `generer_og_mellomlagre_søknad_pdf` | ekstern behovløser | `SøknadPdfGenerertOgMellomlagretMottak` |
| løsning på `journalfør_søknad_pdf_og_vedlegg` | ekstern behovløser | `SøknadPdfOgVedleggJournalførtMottak` |
| løsning på `journalfør_ettersending_av_dokumentasjon` | ekstern behovløser | `MeldingOmEttersendingMottak` |
| `søknadsbehandling_ferdig` | dp-behandling | `SøknadsbehandlingFerdigMottak` |

### Ut

| Event / behov | Sendes fra | Innhold |
|---|---|---|
| `søknad_endret_tilstand` | `SøknadService`, `MeldingOmSøknadKlarTilJournalføringMottak` | Tilstandsendring (PÅBEGYNT, INNSENDT, Slettet) |
| `paabegynt_soknad_seksjon` | `SeksjonService.lagre()` | Statistikk om lagrede seksjoner |
| `søknad_klar_til_journalføring` | `SøknadService.sendInn()` | Starter PDF-kjeden |
| behov `generer_og_mellomlagre_søknad_pdf` | `MeldingOmSøknadKlarTilJournalføringMottak` | PDF-generering |
| behov `journalfør_søknad_pdf_og_vedlegg` | `SøknadPdfGenerertOgMellomlagretMottak` | Journalføring |
| `dokumentkrav_innsendt` | `MeldingOmSøknadKlarTilJournalføringMottak`, `MeldingOmEttersendingMottak` | Dokumentasjonskrav |
| behov `journalfør_ettersending_av_dokumentasjon` | `SeksjonService` | Ettersending |

---

## Viktige konvensjoner

- `slettSøknadOgInkrementerMetrikk(søknadId, ident, kilde)` brukes av begge flyter. Send alltid riktig
  `kilde` (`"ny"` eller `"quiz"`), siden den styrer metrikk-labelen.
- Automatisk sletting bruker `slettSøknadSomSystem()` og `SøknadMetrikker.slettet.labelValues("ny")`.
  Disse søknadene er alltid ny flyt, siden gamle søknader aldri var i PÅBEGYNT.
- `SøknadRepository.hentSoknaderForIdent()` gjør inner join med `seksjon_v2` og left join med
  `soknad_status`. Søknader uten rader i `seksjon_v2` vises derfor ikke i mine søknader. Det gjelder
  gamle quiz-søknader og nye søknader uten lagrede seksjoner.
- Mottak som får `søknadId` og `ident` fra Kafka sjekker at identen eier søknaden før de skriver.
  Følg samme mønster i nye mottak.
