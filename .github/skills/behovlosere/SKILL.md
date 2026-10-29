---
name: behovlosere
description: |
  Behovløsere i dp-soknad-orkestrator — mønstre, typer og steg-for-steg guide for å legge til nye.
  Bruk denne skillen når du skal lage nye behovløsere, forstå hvordan eksisterende fungerer,
  eller trenger å vite hvilken type behovløser som passer for et nytt behov.
---

# Behovløsere i dp-soknad-orkestrator

Sist verifisert mot koden: oktober 2026.

## Hva er en behovløser?

En behovløser lytter på Kafka etter meldinger med `@event_name: "behov"` som inneholder et spesifikt
`@behov`-navn. Når meldingen matches, henter behovløseren den relevante opplysningen og publiserer
tilbake en `@løsning` på rapids.

**Dataflyt:**
```
dp-behandling  →  Kafka (behov)  →  BehovMottak  →  BehovløserFactory  →  Behovløser  →  Kafka (@løsning)
```

`BehovMottak` er en Rapids & Rivers `River.PacketListener` som:
1. Filtrerer på `@event_name = "behov"` og at `@behov` matcher et kjent behov
2. Sjekker at `@løsning` IKKE finnes (unngår dobbeltbehandling)
3. Validerer at meldingen inneholder `ident`, `søknadId`, `behandlingId` og `@behovId`
4. Sjekker at søknaden eksisterer
5. Delegerer til riktig behovløser via `BehovløserFactory`

Innkommende behov-meldinger **må** ha alle disse feltene for å bli plukket opp.

## Datakilde-prioritet

For **vanlige behovløsere** er datakilde-prioriteten som regel:

1. **`quiz_opplysning`-tabellen** — quiz-søknad (gammel flyt). Hentes via `opplysningRepository.hent(beskrivendeId, ident, søknadId)`.
2. **`seksjon_v2`-tabellen** — ny søknad-flyt. Hentes via `seksjonRepository.hentSeksjonsvarEllerKastException(ident, søknadId, seksjonId)`.

Appen tar ikke lenger imot quiz-søknader (fjernet i #431), så quiz-oppslaget treffer bare historiske
søknader. Nye søknader har bare data i `seksjon_v2`. Basisklassen krever likevel en `beskrivendeId`,
og quiz-fallbacken må beholdes så lenge behov for gamle søknader kan komme inn.

Seksjonsdata er JSON med struktur `{ "seksjonId": "...", "seksjonsvar": { ... }, "versjon": N }`.
Feltverdiene ligger inne i `seksjonsvar`. Koden bruker Jacksons `findPath(feltNavn)` som søker
rekursivt — du trenger ikke navigere ned til `seksjonsvar` manuelt.

### Viktige unntak

- **`BarnetilleggBehovLøser` og `BarnetilleggV2BehovLøser`** leser først fra `saksbehandler_barn`, deretter quiz-opplysninger, og til slutt `seksjon_v2`.
- **`SøknadsdatoBehovløser`** faller tilbake til `søknadRepository.hent(søknadId)?.innsendtTidspunkt`.
- **`SøknadsdataBehovløser`** er en spesialvariant som ikke går via `BehovløserFactory`, men via eget mottak og `SøknadBehovmelding`.

## Løsningsformat på Kafka

Alle løsninger publiseres med dette formatet:
```json
{
  "@løsning": {
    "BehovNavn": {
      "verdi": <svar>,
      "gjelderFra": "<dato>"
    }
  }
}
```

`gjelderFra` settes automatisk til søknadstidspunktet (fra quiz-opplysning `søknadstidspunkt` eller
`søknadRepository.hent(søknadId)?.innsendtTidspunkt`). Mangler tidspunktet, blir verdien `null`.
Unntaket er `Søknadsdata`: overloaden med `SøknadBehovmelding` setter alltid `gjelderFra = null`.

## Typer behovløsere

### Type 1: Enkel seksjonsdata-boolean (vanligste)

Bruker basisklassemetoden `løsBehovFraSeksjonsData`. Prøver quiz-repo først, faller tilbake til seksjon.

**Eksempel:** `KanJobbeHvorSomHelstBehovløser`, `EgetGårdsbrukBehovløser`, `EgenNæringsvirksomhetBehovløser`

```kotlin
class MittNyttBehovBehovløser(
    rapidsConnection: RapidsConnection,
    opplysningRepository: QuizOpplysningRepository,
    søknadRepository: SøknadRepository,
    seksjonRepository: SeksjonRepository,
) : Behovløser(rapidsConnection, opplysningRepository, søknadRepository, seksjonRepository) {
    override val behov = MittNyttBehov.name
    override val beskrivendeId = "faktum.mitt-faktum-id"   // Fra quiz-appen, ikke finn opp nye

    override fun løs(behovmelding: Behovmelding) {
        løsBehovFraSeksjonsData(behovmelding, "seksjon-id", "feltnavnISeksjonsJson")
    }
}
```

> Alle behovløsere i kodebasen overstyrer `løs()` eksplisitt. Basisklassens
> standard-implementasjon (som bare gjør quiz-oppslag) brukes ikke direkte av noen.

### Type 2: Delt logikk via `FellesBehovløserLøsninger`

Noen behovløsere deler logikk via `FellesBehovløserLøsninger`. Disse tar et ekstra
`fellesBehovløserLøsninger`-parameter i konstruktøren.

**Eksempel:** `VernepliktBehovløser`, `EØSArbeidBehovløser`, `ØnskerDagpengerFraDatoBehovløser`

Tilgjengelige fellesmetoder:
- `harSøkerenHattArbeidsforholdIEøs(beskrivendeId, ident, søknadId): Boolean`
- `ønskerDagpengerFraDato(ident, søknadId, behov): LocalDate`
- `harSøkerenAvtjentVerneplikt(behov, beskrivendeId, ident, søknadId): Boolean`

### Type 3: Tilpasset logikk med egne kilder eller domeneregler

Henter fra begge kilder manuelt og gjør domenelogikk. Bruker `publiserLøsning(behovmelding, svar)`.

**Eksempel:** `OrdinærBehovløser`, `BostedslandErNorgeBehovløser`, `SøknadsdatoBehovløser`,
`BarnetilleggBehovLøser`, `BarnetilleggV2BehovLøser`

```kotlin
override fun løs(behovmelding: Behovmelding) {
    val svarPåBehov = beregnSvar(behovmelding.ident, behovmelding.søknadId)
    publiserLøsning(behovmelding, svarPåBehov)
}

internal fun beregnSvar(ident: String, søknadId: UUID): Boolean {
    val fraQuiz = opplysningRepository.hent(beskrivendeId, ident, søknadId)
    if (fraQuiz != null) {
        return fraQuiz.svar as Boolean   // eller konverter
    }
    val seksjonsSvar = seksjonRepository.hentSeksjonsvarEllerKastException(ident, søknadId, "seksjonId")
    objectMapper.readTree(seksjonsSvar).let { json ->
        json.findPath("feltNavn")?.let {
            if (!it.isMissingOrNull()) return it.erBoolean()
        }
    }
    throw IllegalStateException("Fant ingen opplysning på behov $behov for søknad med id: $søknadId")
}
```

### Type 4: Eget mottak og `SøknadBehovmelding`

Brukes når behovet **ikke følger standardkontrakten** med `søknadId` i meldingen, eller trenger
egne preconditions og eget mottak.

**Eksempel:** `SøknadsdataBehovløser` + `SøknadsdataBehovMottak`

Kjennetegn:
- behovet ligger **ikke** i `BehovløserFactory.Behov`
- du bruker `override fun løs(behovmelding: SøknadBehovmelding)`
- du trenger et eget mottak som registreres i `ApplicationBuilder`
- testmeldinger lages med `lagBehovmeldingUtenSøknadId(ident)` fra `PacketGenerator.kt`

`SøknadsdataBehovMottak` har en hardkodet `behovIdSkipSet` med `@behovId`-er som ignoreres. Den brukes
til å hoppe over enkeltmeldinger som feiler i prod (#516). Dette er et midlertidig driftstiltak.
Avklar endringer i listen særskilt med teamet, ikke endre den som del av generell opprydding.

## Steg-for-steg: Legge til en ny behovløser

### 0. Avklar hvilken kategori behovet tilhører

Start alltid med dette:

1. **Har meldingen `søknadId` og følger vanlig behov-kontrakt?** Bruk `BehovMottak` + `BehovløserFactory`.
2. **Er det i praksis en enkel boolean med quiz/seksjon-fallback?** Start med `løsBehovFraSeksjonsData`.
3. **Trenger det egne kilder, defaults eller spesiallogikk?** Lag tilpasset `løs()`.
4. **Mangler meldingen `søknadId` eller har egne preconditions?** Følg `SøknadsdataBehovMottak`-mønsteret i stedet for factory.

### 1. Lag behovløseren

Opprett `src/main/kotlin/.../behov/løsere/MittNyttBehovBehovløser.kt`.
For en enkel boolean, bruk eksemplet under Type 1 og
`behov/løsere/KanJobbeHvorSomHelstBehovløser.kt` som utgangspunkt.
Bruk verifiserte verdier for `beskrivendeId`, seksjons-ID og feltnavn.

### 2. Legg til i `Behov`-enum i `BehovløserFactory.kt`

```kotlin
enum class Behov {
    // ... eksisterende ...
    MittNyttBehov,
}
```

### 3. Registrer i factory-mapet i `BehovløserFactory.kt`

```kotlin
MittNyttBehov to MittNyttBehovBehovløser(
    rapidsConnection,
    opplysningRepository,
    søknadRepository,
    seksjonRepository,
),
```

Hvis behovløseren trenger `fellesBehovløserLøsninger`, legg det til som siste argument.

Legg også til nødvendig `import` for den nye behovløser-klassen.

> Gjelder bare vanlige behov som går via `BehovMottak`. For spesialtilfeller som `Søknadsdata`
> skal du ikke legge behovet i `BehovløserFactory`, men lage/oppdatere eget mottak og wiring i
> `ApplicationBuilder`.

### 4. Oppdater `BehovløserFactoryTest.kt`

Legg til `"MittNyttBehov"` i listen i `Kan hente ut alle behov`-testen:

```kotlin
behovløserFactory.behov().shouldContainExactlyInAnyOrder(
    // ... eksisterende ...
    "MittNyttBehov",
)
```

### 5. Skriv test

Opprett `src/test/kotlin/.../behov/løsere/MittNyttBehovBehovløserTest.kt`.
Følg `src/test/kotlin/no/nav/dagpenger/soknad/orkestrator/behov/løsere/KanJobbeHvorSomHelstBehovløserTest.kt`
for en enkel boolean med quiz/seksjon-fallback. Den viser oppsett med `TestRapid`,
quiz-data og mockede seksjonsdata.

Dekk løsning fra hver støttet kilde, forventet `verdi` og `gjelderFra`, og oppførselen ved
manglende data etter behovets kontrakt.

Test-hjelpere tilgjengelig:
- `lagBehovmelding(ident, søknadId, behov)` — fra `PacketGenerator.kt`
- `lagBehovmeldingUtenSøknadId(ident)` — fra `PacketGenerator.kt`, for behov av type 4
- `InMemoryQuizOpplysningRepository` — in-memory repo for quiz-opplysninger
- bruk eksplisitt `Søknad(...)` når testen trenger fallback til `gjelderFra` fra `søknadRepository`

## Nøkkelfiler

| Fil | Formål |
|-----|--------|
| `behov/Behovløser.kt` | Abstrakt basisklasse. Inneholder `løsBehovFraSeksjonsData`, `publiserLøsning`, `finnGjelderFraDato` |
| `behov/BehovløserFactory.kt` | `Behov`-enum + mapping til instanser |
| `behov/BehovMottak.kt` | Rapids & Rivers listener. Ruter innkommende behovmeldinger |
| `behov/SøknadsdataBehovMottak.kt` | Eget mottak for behov som ikke følger standardkontrakten med `søknadId` |
| `behov/FellesBehovløserLøsninger.kt` | Delt logikk for EØS, verneplikt, søknadsdato |
| `behov/Behovmelding.kt` | Wrappere rundt `JsonMessage`: `Behovmelding` for vanlige behov og `SøknadBehovmelding` for behov uten `søknadId` |
| `behov/løsere/*.kt` | Én fil per behovløser |
| `test/.../behov/løsere/PacketGenerator.kt` | `lagBehovmelding(...)` og `lagBehovmeldingUtenSøknadId(...)` |
| `metrikker/Metrikker.kt` | `BehovMetrikker.mottatt` (i `BehovMottak`) og `BehovMetrikker.løst` (i `publiserLøsning`) |
| `test/.../utils/InMemoryQuizOpplysningRepository.kt` | In-memory repo for tester |

## Viktige konvensjoner

- **`beskrivendeId`-verdier** (f.eks. `faktum.jobbe-hele-norge`) defineres av quiz-appen. Ikke finn opp nye — bruk det som allerede finnes i inkommende data.
- **Feilhåndtering følger behovets kontrakt**: mange boolean-behov kaster `IllegalStateException`, men noen behov har bevisste defaults eller tomme svar (`ØnsketArbeidstid`, `Barnetillegg`, `Søknadsdata`).
- **Boolean i seksjonsvar** er strenger `"ja"`/`"nei"` — bruk `it.erBoolean()` (extension i `utils/`).
- **`gjelderFra`** settes av `publiserLøsning` via `finnGjelderFraDato` for vanlige behov.
  For `SøknadBehovmelding` settes verdien alltid til `null`.
- **`Søknadsdata` er et unntak** — det bruker `SøknadBehovmelding`, eget mottak og ligger ikke i `BehovløserFactory.Behov`.
- **Logger:** bruk `logger` for normal logging, `sikkerlogg` hvis du logger persondata (ident, svar).
- **Navngivning:** klassen heter `<BehovNavn>Behovløser`, filen tilsvarende. Unntak: `BarnetilleggBehovLøser` / `BarnetilleggV2BehovLøser` bruker `BehovLøser` (historisk inkonsistens — følg `Behovløser`-mønsteret for nye).

## Håndtering av manglende seksjoner

Bevar oppførselen til det konkrete behovet. Noen behovløsere kaster exception, mens andre
returnerer `false` eller en tom liste. Avklar behovets kontrakt før du innfører eller endrer
en default. Ikke standardiser feilhåndteringen på tvers av behov som del av en avgrenset endring.

## Feil å unngå

- Glem ikke å legge til i **`BehovløserFactoryTest`** — testen feiler ellers.
- Ikke anta at alle behov skal i `BehovløserFactory` — behov uten `søknadId` eller med egne preconditions kan trenge eget mottak.
- Ikke hardkod svar uten å sjekke hvilke kilder den konkrete behovløseren faktisk skal støtte.
- Ved vanlige oppslag i quiz-opplysninger for et behov, bruk som hovedregel tre-argument-varianten `hent(beskrivendeId, ident, søknadId)`.
- `løsBehovFraSeksjonsData` håndterer fallback selv — ikke dupliser logikken manuelt.
