---
name: behovlosere
description: |
  Behovløsere i dp-soknad-orkestrator — mønstre, typer og steg-for-steg guide for å legge til nye.
  Bruk denne skillen når du skal lage nye behovløsere, forstå hvordan eksisterende fungerer,
  eller trenger å vite hvilken type behovløser som passer for et nytt behov.
---

# Behovløsere i dp-soknad-orkestrator

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
`søknadRepository.hent(søknadId)?.innsendtTidspunkt`).

## Typer behovløsere

### Type 1: Enkel seksjonsdata-boolean (vanligste)

Bruker basisklassemetoden `løsBehovFraSeksjonsData`. Prøver quiz-repo først, faller tilbake til seksjon.

**Eksempel:** `KanJobbeHvorSomHelstBehovløser`, `EgetGårdsbrukBehovløser`, `EgenNæringsvirksomhetBehovløser`

```kotlin
class MittBehovBehovløser(
    rapidsConnection: RapidsConnection,
    opplysningRepository: QuizOpplysningRepository,
    søknadRepository: SøknadRepository,
    seksjonRepository: SeksjonRepository,
) : Behovløser(rapidsConnection, opplysningRepository, søknadRepository, seksjonRepository) {
    override val behov = MittBehov.name
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

## Steg-for-steg: Legge til en ny behovløser

### 0. Avklar hvilken kategori behovet tilhører

Start alltid med dette:

1. **Har meldingen `søknadId` og følger vanlig behov-kontrakt?** Bruk `BehovMottak` + `BehovløserFactory`.
2. **Er det i praksis en enkel boolean med quiz/seksjon-fallback?** Start med `løsBehovFraSeksjonsData`.
3. **Trenger det egne kilder, defaults eller spesiallogikk?** Lag tilpasset `løs()`.
4. **Mangler meldingen `søknadId` eller har egne preconditions?** Følg `SøknadsdataBehovMottak`-mønsteret i stedet for factory.

### 1. Lag behovløseren

Opprett `src/main/kotlin/.../behov/løsere/MittNyttBehovBehovløser.kt`:

```kotlin
package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.dagpenger.soknad.orkestrator.behov.Behovløser
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.MittNyttBehov
import no.nav.dagpenger.soknad.orkestrator.behov.Behovmelding
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonRepository

class MittNyttBehovBehovløser(
    rapidsConnection: RapidsConnection,
    opplysningRepository: QuizOpplysningRepository,
    søknadRepository: SøknadRepository,
    seksjonRepository: SeksjonRepository,
) : Behovløser(rapidsConnection, opplysningRepository, søknadRepository, seksjonRepository) {
    override val behov = MittNyttBehov.name
    override val beskrivendeId = "faktum.mitt-faktum-id"

    override fun løs(behovmelding: Behovmelding) {
        løsBehovFraSeksjonsData(behovmelding, "seksjon-id", "feltNavn")
    }
}
```

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

Opprett `src/test/kotlin/.../behov/løsere/MittNyttBehovBehovløserTest.kt`:

```kotlin
package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.QuizOpplysning
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Boolsk
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Tekst
import no.nav.dagpenger.soknad.orkestrator.søknad.Søknad
import no.nav.dagpenger.soknad.orkestrator.søknad.Tilstand
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonRepository
import no.nav.dagpenger.soknad.orkestrator.utils.InMemoryQuizOpplysningRepository
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.test.Test

class MittNyttBehovBehovløserTest {
    val opplysningRepository = InMemoryQuizOpplysningRepository()
    val testRapid = TestRapid()
    val søknadRepository = mockk<SøknadRepository>(relaxed = true)
    val seksjonRepository = mockk<SeksjonRepository>(relaxed = true)
    val behovløser = MittNyttBehovBehovløser(testRapid, opplysningRepository, søknadRepository, seksjonRepository)
    val ident = "12345678910"
    val søknadId = UUID.randomUUID()

    @Test
    fun `skal publisere løsning fra quiz-opplysning`() {
        opplysningRepository.lagre(
            QuizOpplysning(
                beskrivendeId = behovløser.beskrivendeId,
                type = Boolsk,
                svar = true,
                ident = ident,
                søknadId = søknadId,
            ),
        )
        val søknadstidspunkt = ZonedDateTime.now()
        opplysningRepository.lagre(
            QuizOpplysning(
                beskrivendeId = "søknadstidspunkt",
                type = Tekst,
                svar = søknadstidspunkt.toString(),
                ident = ident,
                søknadId = søknadId,
            ),
        )

        behovløser.løs(lagBehovmelding(ident, søknadId, BehovløserFactory.Behov.MittNyttBehov))

        testRapid.inspektør.message(0)["@løsning"]["MittNyttBehov"].also { løsning ->
            løsning["verdi"].asBoolean() shouldBe true
            løsning["gjelderFra"].asLocalDate() shouldBe søknadstidspunkt.toLocalDate()
        }
    }

    @Test
    fun `skal publisere løsning fra seksjonsdata`() {
        every { seksjonRepository.hentSeksjonsvarEllerKastException(any(), any(), any()) } returns
            """
            {
              "seksjonId": "seksjon-id",
              "seksjonsvar": { "feltNavn": "ja" },
              "versjon": 1
            }
            """.trimIndent()
        every { søknadRepository.hent(any()) } returns
            Søknad(
                søknadId = søknadId,
                ident = ident,
                tilstand = Tilstand.INNSENDT,
                innsendtTidspunkt = ZonedDateTime.now().toLocalDateTime(),
            )

        behovløser.løs(lagBehovmelding(ident, søknadId, BehovløserFactory.Behov.MittNyttBehov))

        verify { seksjonRepository.hentSeksjonsvarEllerKastException(ident, søknadId, "seksjon-id") }
        testRapid.inspektør.message(0)["@løsning"]["MittNyttBehov"]["verdi"].asBoolean() shouldBe true
    }
}
```

Test-hjelpere tilgjengelig:
- `lagBehovmelding(ident, søknadId, behov)` — fra `PacketGenerator.kt`
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
| `test/.../behov/løsere/PacketGenerator.kt` | `lagBehovmelding(...)` test-helper |
| `test/.../utils/InMemoryQuizOpplysningRepository.kt` | In-memory repo for tester |

## Viktige konvensjoner

- **`beskrivendeId`-verdier** (f.eks. `faktum.jobbe-hele-norge`) defineres av quiz-appen. Ikke finn opp nye — bruk det som allerede finnes i inkommende data.
- **Feilhåndtering følger behovets kontrakt**: mange boolean-behov kaster `IllegalStateException`, men noen behov har bevisste defaults eller tomme svar (`ØnsketArbeidstid`, `Barnetillegg`, `Søknadsdata`).
- **Boolean i seksjonsvar** er strenger `"ja"`/`"nei"` — bruk `it.erBoolean()` (extension i `utils/`).
- **`gjelderFra`** trenger du ikke sette manuelt — `publiserLøsning` gjør det via `finnGjelderFraDato`.
- **`Søknadsdata` er et unntak** — det bruker `SøknadBehovmelding`, eget mottak og ligger ikke i `BehovløserFactory.Behov`.
- **Logger:** bruk `logger` for normal logging, `sikkerlogg` hvis du logger persondata (ident, svar).
- **Navngivning:** klassen heter `<BehovNavn>Behovløser`, filen tilsvarende. Unntak: `BarnetilleggBehovLøser` / `BarnetilleggV2BehovLøser` bruker `BehovLøser` (historisk inkonsistens — følg `Behovløser`-mønsteret for nye).

## Åpne spørsmål

### Håndtering av manglende seksjoner

Det er ikke en enhetlig strategi i kodebasen for hva som skal skje når en seksjon mangler:

- Noen behovløsere kaster exception (`hentSeksjonsvarEllerKastException`) — appen feiler og det blir synlig
- Noen returnerer en fallback-verdi (f.eks. `false`) — stille og usynlig
- Noen har domenespesifikke defaults (f.eks. tom liste)

**Spørsmål som bør avklares:** Når er det riktig å feile hardt vs. falle tilbake til en default?
Avhenger trolig av om behovet er et filter (der `false` er en meningsfull verdi) eller et faktum
(der manglende data er et tegn på noe galt). Bør diskuteres og standardiseres.

## Feil å unngå

- Glem ikke å legge til i **`BehovløserFactoryTest`** — testen feiler ellers.
- Ikke anta at alle behov skal i `BehovløserFactory` — behov uten `søknadId` eller med egne preconditions kan trenge eget mottak.
- Ikke hardkod svar uten å sjekke hvilke kilder den konkrete behovløseren faktisk skal støtte.
- Ved vanlige oppslag i quiz-opplysninger for et behov, bruk som hovedregel tre-argument-varianten `hent(beskrivendeId, ident, søknadId)`.
- `løsBehovFraSeksjonsData` håndterer fallback selv — ikke dupliser logikken manuelt.
