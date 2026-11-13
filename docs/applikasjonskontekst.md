# Applikasjonskontekst

Dette dokumentet beskriver hva dp-soknad-orkestrator gjør, hvilke data og meldinger
appen eier, og hva en agent bør vurdere før den foreslår endringer på tvers av
applikasjoner.

## Kort beskrivelse

dp-soknad-orkestrator er backend for dagpengesøknaden. Appen oppretter søknader,
lagrer svar, dokumentasjonskrav og vedtaksstatus, publiserer søknads- og tilstandshendelser,
starter journalføringskjeden og svarer på informasjonsbehov fra dp-behandling.

Appen er ikke frontend. Den bestemmer heller ikke hvilke spørsmål innbyggeren får,
hvordan skjermbildene ser ut, eller hvordan dp-behandling fatter vedtak.

## Hvem bruker appen

| Konsument eller samarbeidspartner | Bruk |
|---|---|
| `dp-brukerdialog-frontend` | Oppretter søknad, lagrer seksjoner, viser framdrift og sender inn |
| `dp-mine-dagpenger-frontend` | Henter egne søknader og dokumentasjonskrav |
| `dp-behandling` | Ber om opplysninger og melder `søknadsbehandling_ferdig` |
| Rapids & Rivers på `teamdagpenger.rapid.v1` | Transport for behov, løsninger og søknadshendelser |
| PDL | Henter personalia og barn for innbygger |
| Person-/kontoregisteret | Henter aktivt kontonummer |
| SAF | Finner søknadsdata ved spesielle behov fra journalførte dokumenter |
| PDF- og journalføringstjenester | Genererer, mellomlagrer og journalfører søknad og vedlegg |

## Ansvar

Appen har ansvar for:

- søknadens livssyklus: `PÅBEGYNT`, `INNSENDT`, `JOURNALFØRT` og `SLETTET_AV_SYSTEM`
- lagring av søknader, seksjonssvar, PDF-grunnlag, dokumentasjonskrav og vedtaksstatus fra dp-behandling
- overgangsstøtte mellom quiz-opplysninger og `seksjon_v2`
- å svare på behov fra dp-behandling med data fra riktig kilde
- å publisere hendelser om søknader, tilstander og dokumentasjonskrav
- lesing av historiske barneendringer fra `saksbehandler_barn` i barn-behovløserne (skriveflyten er avviklet)
- ettersending av dokumentasjon og kobling mot journalpost

Appen har ikke ansvar for:

- spørsmål, rekkefølge eller validering i innbyggergrensesnittet
- opplasting av filer og brukerens visning av dokumentasjonskrav
- vedtakslogikk eller behandlingens tilstand i dp-behandling
- selve PDF-genereringen og journalføringen hos nedstrøms tjenester

## Søknadsflyter

### Ny seksjonsbasert flyt

```text
Frontend
  -> POST /soknad
  -> PUT /seksjon/{søknadId}/{seksjonId}
  -> POST /soknad/{søknadId}
  -> søknad_klar_til_journalføring
  -> generer_og_mellomlagre_søknad_pdf
  -> journalfør_søknad_pdf_og_vedlegg
  -> JOURNALFØRT
```

Søknaden starter i `PÅBEGYNT`. `SøknadService.sendInn()` publiserer
`søknad_klar_til_journalføring`. Mottaket validerer identitet og tilstand, markerer
søknaden som innsendt og publiserer behov for PDF-generering. Når journalføringen
er bekreftet, markeres søknaden som `JOURNALFØRT`.

En søknad som fortsatt er `PÅBEGYNT` kan slettes av innbyggeren. En periodisk jobb
setter også søknader som ikke er oppdatert på sju dager til `SLETTET_AV_SYSTEM`.

### Gammel quiz-flyt

Quiz-flyten kommer inn via Kafka fra `dp-soknad`. Opplysningene ligger i
`quiz_opplysning`, og søknaden kommer normalt inn som allerede innsendt.
`SøknadSlettetMottak` håndterer sletting fra den gamle flyten.

Behovløsere må derfor støtte begge datakildene. For de fleste behov prøves
quiz-opplysninger først, før det falles tilbake til `seksjon_v2`.

## API-overflate

| Område | Endepunkter | Auth | Hovedformål |
|---|---|---|---|
| Søknad | `POST /soknad`, `POST/DELETE /soknad/{id}` | TokenX | Opprette, sende inn og slette søknad |
| Søknader | `GET /soknad/mine-soknader` | TokenX | Hente søkerens søknader, med tittel, manglende dokumentasjonskrav og vedtaksresultat |
| Seksjoner | `GET /seksjon/{søknadId}`, `GET/PUT /seksjon/{søknadId}/{seksjonId}` | TokenX | Lese og lagre seksjonssvar |
| Framdrift | `GET /soknad/{id}/progress`, `GET /soknad/{id}/sistoppdatert` | TokenX | Hente lagrede seksjoner og sist oppdatert-tidspunkt |
| Personalia på søknad | `PUT /soknad/{id}/personalia` | TokenX | Lagre personalia på søknaden |
| Dokumentasjonskrav | `GET /soknad/{id}/dokumentasjonskrav`, `PUT /seksjon/{søknadId}/{seksjonId}/dokumentasjonskrav` | TokenX | Hente og lagre krav knyttet til søknaden |
| Ettersending | `PUT /seksjon/{søknadId}/{seksjonId}/dokumentasjonskrav/ettersending` | TokenX | Starte journalføring av ettersendt dokumentasjon |
| Personalia | `GET /personalia` | TokenX | Hente personalia og kontonummer |
| Barn | `GET /barn` | TokenX | Hente barn fra PDL |
| Land | `GET /land` | Ingen | Hente landliste. Brukes av dp-saksbehandling-frontend |

OpenAPI-spesifikasjonen i `openapi/src/main/resources/soknad-orkestrator-api.yaml`
er kilden for genererte DTO-er. Genererte filer skal ikke endres direkte.

## Data og kildeeierskap

| Data | Tabell eller kilde | Eier i denne appen |
|---|---|---|
| Søknad og tilstand | `soknad` | Ja |
| Ny søknads svar | `seksjon_v2` | Ja |
| Ny søknads PDF-grunnlag | `seksjon_v2.pdf_grunnlag` | Ja |
| Vedtaksstatus | `soknad_status` | Ja, fylt av `søknadsbehandling_ferdig` |
| Dokumentasjonskrav | `seksjon_v2.dokumentasjonskrav` | Ja |
| Gamle quiz-svar | `quiz_opplysning` | Ja, for lesing og kompatibilitet |
| Saksbehandlerens barn (historikk, avviklet skriveflyt) | `saksbehandler_barn` | Ja, append-only snapshot, bare lesing |
| Barn og persondata | PDL og kontoregister | Nei, eksterne kilder |
| Journalpost og dokument | Journalføring/SAF | Nei |

`SøknadRepository.hentSoknaderForIdent()` bruker en inner join mot `seksjon_v2`. Søknader uten
lagrede seksjoner blir derfor ikke med i oversikten over mine søknader.

## Kafka-kontrakter

### Appen mottar

- `søknad_slettet` fra den gamle søknadsflyten
- `søknad_klar_til_journalføring`
- løsning på `generer_og_mellomlagre_søknad_pdf`
- løsning på `journalfør_søknad_pdf_og_vedlegg`
- løsning på `journalfør_ettersending_av_dokumentasjon`
- behov fra dp-behandling, blant annet barn, arbeid, permittering og andre
  søknadsopplysninger
- `søknadsbehandling_ferdig` fra dp-behandling, som lagrer vedtaksstatus i `soknad_status`

### Appen publiserer

- `søknad_endret_tilstand`
- `søknad_klar_til_journalføring`
- `dokumentkrav_innsendt`
- `søknad_slettet`
- `generer_og_mellomlagre_søknad_pdf`
- `journalfør_søknad_pdf_og_vedlegg`
- `journalfør_ettersending_av_dokumentasjon`
- løsninger på behov fra dp-behandling

Den komplette behovlisten og løsningsformatet ligger i
`asyncapi/src/main/resources/async-api.yaml`, men spesialbehov og ny
søknadsflyt må også leses fra mottakene i `src/main/kotlin`.

## Dokumentasjonskrav og ettersending

Ved lagring av en seksjon kan frontend sende med dokumentasjonskrav. Ved innsending
publiserer appen `dokumentkrav_innsendt` og sender kravene videre i PDF- og
journalføringskjeden. Krav der innbyggeren har valgt «send senere» kan hentes via
`GET /soknad/{id}/dokumentasjonskrav`.

Ved ettersending må en klient lagre nye krav med ettersendingsendepunktet. Appen
publiserer da behov for journalføring av dokumentasjonen. Når journalføringen er
bekreftet, lagres journalposten og appen publiserer `dokumentkrav_innsendt` på nytt.

Hvis målet er å øke andelen som sender inn dokumentasjon, bør en tverrgående
vurdering starte i `dp-brukerdialog-frontend` og `dp-mine-dagpenger-frontend`.
Denne appen er relevant for:

- hvilke dokumentasjonskrav som lagres og publiseres
- om «send senere» og ettersending har riktig livssyklus
- framdrift og søknadsoversikt som frontend bruker for å vise mangler
- metrikk og hendelser som kan måle overgangene i dokumentasjonsflyten
- feil i journalføringskjeden som gjør at innsendt dokumentasjon ikke blir registrert

Appen kan ikke alene endre tekst, plassering eller påminnelser i brukergrensesnittet.

## Tilgang, personvern og endringsgrenser

- TokenX beskytter innbyggerendepunktene.
- Azure AD brukes bare for tjeneste-til-tjeneste-kall ut fra appen (for eksempel SAF). Appen har ingen
  Azure AD-beskyttede endepunkter. Gruppen `saksbehandler` står igjen i Nais-manifestet fordi den styrer
  hvem som får OBO-token for appen. Frontendens tokenutveksling for `/land` må fjernes før gruppen kan fjernes.
- Fødselsnummer, ident og svar skal ikke logges i vanlig logg. Kode som trenger
  persondata bruker sikkerlogg.
- Barn-behovløserne leser siste snapshot fra `saksbehandler_barn`. Appen skriver ikke lenger nye snapshots.
  Tabellen skal ikke oppdateres rad for rad.
- Endringer i OpenAPI- eller AsyncAPI-kontrakter må vurderes mot alle konsumenter.
- Behovløsere må beholde støtte for både quiz-opplysninger og `seksjon_v2` så lenge
  den gamle flyten fortsatt finnes.
- Endringer i søknadstilstander, Kafka-eventer, auth eller databaseformat er
  høyrisiko og bør vurderes som tverrgående endringer.

## Metrikker som kan brukes i vurderinger

Metrikkene ligger under `dp_soknad_orkestrator`. De viktigste for søknadstrakten er:

- `antall_soknader_opprettet`
- `antall_soknader_mottatt{kilde="ny"}`
- `antall_soknader_journalfort`
- `antall_soknader_slettet{kilde="ny"}`
- `antall_behov_mottatt{behov}`
- `antall_behov_lost{behov}`
- `antall_soknader_varslet`
- `antall_soknader_dekomponert` og `antall_soknader_dekomponering_feilet`

Metrikkene viser søknadsovergangene, men ikke nødvendigvis brukerens eksponering
for en tekst eller påminnelse. For produktmål må de kombineres med frontend- eller
analysehendelser.

## Kildekode som bør leses ved endringer

| Tema | Start her |
|---|---|
| Oppretting, innsending og sletting | `søknad/SøknadApi.kt`, `søknad/SøknadService.kt` |
| Seksjoner og dokumentasjonskrav | `søknad/seksjon/SeksjonApi.kt`, `SeksjonService.kt` |
| PDF og journalføring | `søknad/melding/`, `søknad/mottak/`, `søknad/pdf/` |
| Behov og svar | `behov/BehovMottak.kt`, `behov/BehovløserFactory.kt`, `behov/løsere/` |
| Historiske barn (bare lesing) | `opplysning/SaksbehandlerBarnRepository.kt`, `behov/løsere/BarnetilleggV2BehovLøser.kt` |
| Vedtaksstatus | `søknad/mottak/SøknadsbehandlingFerdigMottak.kt`, `søknad/db/SøknadStatusRepository.kt` |
| Auth og eksterne klienter | `api/auth/`, `ApplicationBuilder.kt`, `Configuration.kt` |
| Kontrakter | `openapi/src/main/resources/`, `asyncapi/src/main/resources/` |
