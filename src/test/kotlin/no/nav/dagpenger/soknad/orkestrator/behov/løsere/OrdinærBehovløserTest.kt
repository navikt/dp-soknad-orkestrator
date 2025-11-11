package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.Ordinær
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.QuizOpplysning
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Arbeidsforhold
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.ArbeidsforholdSvar
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Sluttårsak
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Tekst
import no.nav.dagpenger.soknad.orkestrator.søknad.Søknad
import no.nav.dagpenger.soknad.orkestrator.søknad.Tilstand
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonRepository
import no.nav.dagpenger.soknad.orkestrator.utils.InMemoryQuizOpplysningRepository
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.test.Test

@Suppress("ktlint:standard:max-line-length")
class OrdinærBehovløserTest {
    val opplysningRepository = InMemoryQuizOpplysningRepository()
    val søknadRepository = mockk<SøknadRepository>(relaxed = true)
    val seksjonRepository = mockk<SeksjonRepository>(relaxed = true)
    val testRapid = TestRapid()
    val behovløser = OrdinærBehovløser(testRapid, opplysningRepository, søknadRepository, seksjonRepository)
    val ident = "12345678910"
    val søknadId = UUID.randomUUID()

    @Test
    fun `Behovløser publiserer løsning på behov Ordinær med verdi og gjelderFra`() {
        opplysningRepository.lagre(opplysning())

        // Må også lagre søknadstidspunkt fordi det er denne som brukes for å sette gjelderFra i første omgang
        val søknadstidspunkt = ZonedDateTime.now()
        val søknadstidpsunktOpplysning =
            QuizOpplysning(
                beskrivendeId = "søknadstidspunkt",
                type = Tekst,
                svar = søknadstidspunkt.toString(),
                ident = ident,
                søknadId = søknadId,
            )

        opplysningRepository.lagre(søknadstidpsunktOpplysning)
        behovløser.løs(lagBehovmelding(ident, søknadId, Ordinær))

        testRapid.inspektør.message(0)["@løsning"]["Ordinær"].also { løsning ->
            løsning["verdi"].asBoolean() shouldBe true
            løsning["gjelderFra"].asLocalDate() shouldBe søknadstidspunkt.toLocalDate()
        }
    }

    @Test
    fun `Behovløser publiserer løsning på behov Ordinær med verdi og gjelderFra fra seksjonsdata med ikke ordinær arbeidsforhold avslutning`() {
        every {
            seksjonRepository.hentSeksjonsvarEllerKastException(
                any(),
                any(),
                any(),
            )
        } returns
            """
            {
                "seksjonId": "arbeidsforhold",
                "seksjon": {
                "registrerteArbeidsforhold": [
                    {
                        "hvordan-har-dette-arbeidsforholdet-endret-seg": "arbeidsgiver-er-konkurs"
                    },
                    {
                        "hvordan-har-dette-arbeidsforholdet-endret-seg": "jeg-er-permitert"
                    }
                ]
                },
                "versjon": 1
            }
            """.trimIndent()

        // Må også lagre søknadstidspunkt fordi det er denne som brukes for å sette gjelderFra i første omgang
        val søknadstidspunkt = ZonedDateTime.now()
        every {
            søknadRepository.hent(any())
        } returns
            Søknad(
                søknadId = søknadId,
                ident = ident,
                tilstand = Tilstand.INNSENDT,
                innsendtTidspunkt = søknadstidspunkt.toLocalDateTime(),
            )
        behovløser.løs(lagBehovmelding(ident, søknadId, Ordinær))

        verify { seksjonRepository.hentSeksjonsvarEllerKastException(ident, søknadId, "arbeidsforhold") }
        verify { søknadRepository.hent(søknadId) }
        testRapid.inspektør.message(0)["@løsning"]["Ordinær"].also { løsning ->
            løsning["verdi"].asBoolean() shouldBe false
            løsning["gjelderFra"].asLocalDate() shouldBe søknadstidspunkt.toLocalDate()
        }
    }

    @Test
    fun `Behovløser publiserer løsning på behov Ordinær med verdi og gjelderFra fra seksjonsdata med bare ordinære arbeidsforhold`() {
        every {
            seksjonRepository.hentSeksjonsvarEllerKastException(
                any(),
                any(),
                any(),
            )
        } returns
            """
            {
                "seksjonId": "arbeidsforhold",
                "seksjon": {
                "registrerteArbeidsforhold": [
                    {
                        "hvordan-har-dette-arbeidsforholdet-endret-seg": "kontrakten-er-ugått"
                    },
                    {
                        "hvordan-har-dette-arbeidsforholdet-endret-seg": "arbeidsgiver-er-konkurs"
                    }
                ]
                },
                "versjon": 1
            }
            """.trimIndent()

        // Må også lagre søknadstidspunkt fordi det er denne som brukes for å sette gjelderFra i første omgang
        val søknadstidspunkt = ZonedDateTime.now()
        every {
            søknadRepository.hent(any())
        } returns
            Søknad(
                søknadId = søknadId,
                ident = ident,
                tilstand = Tilstand.INNSENDT,
                innsendtTidspunkt = søknadstidspunkt.toLocalDateTime(),
            )
        behovløser.løs(lagBehovmelding(ident, søknadId, Ordinær))

        verify { seksjonRepository.hentSeksjonsvarEllerKastException(ident, søknadId, "arbeidsforhold") }
        verify { søknadRepository.hent(søknadId) }
        testRapid.inspektør.message(0)["@løsning"]["Ordinær"].also { løsning ->
            løsning["verdi"].asBoolean() shouldBe true
            løsning["gjelderFra"].asLocalDate() shouldBe søknadstidspunkt.toLocalDate()
        }
    }

    @Test
    fun `Behovløser publiserer løsning på behov Ordinær med verdi og gjelderFra fra seksjonsdata uten arbeidsforhold`() {
        every {
            seksjonRepository.hentSeksjonsvarEllerKastException(
                any(),
                any(),
                any(),
            )
        } returns
            """
            {
                "seksjonId": "arbeidsforhold",
                "seksjon": {
                "registrerteArbeidsforhold": []
                },
                "versjon": 1
            }
            """.trimIndent()

        // Må også lagre søknadstidspunkt fordi det er denne som brukes for å sette gjelderFra i første omgang
        val søknadstidspunkt = ZonedDateTime.now()
        every {
            søknadRepository.hent(any())
        } returns
            Søknad(
                søknadId = søknadId,
                ident = ident,
                tilstand = Tilstand.INNSENDT,
                innsendtTidspunkt = søknadstidspunkt.toLocalDateTime(),
            )
        behovløser.løs(lagBehovmelding(ident, søknadId, Ordinær))

        verify { seksjonRepository.hentSeksjonsvarEllerKastException(ident, søknadId, "arbeidsforhold") }
        verify { søknadRepository.hent(søknadId) }
        testRapid.inspektør.message(0)["@løsning"]["Ordinær"].also { løsning ->
            løsning["verdi"].asBoolean() shouldBe false
            løsning["gjelderFra"].asLocalDate() shouldBe søknadstidspunkt.toLocalDate()
        }
    }

    @Test
    fun `Behovløser setter løsning til true når minst ett arbeidsforhold har en ordinær sluttårsak`() {
        val svarMedIkkeOrdinærRettighetstype =
            listOf(
                ArbeidsforholdSvar(
                    navn = "arbeidsforhold",
                    land = "NOR",
                    sluttårsak = Sluttårsak.KONTRAKT_UTGAATT,
                ),
                ArbeidsforholdSvar(
                    navn = "arbeidsforhold",
                    land = "NOR",
                    sluttårsak = Sluttårsak.PERMITTERT,
                ),
            )

        opplysningRepository.lagre(opplysning(svar = svarMedIkkeOrdinærRettighetstype))
        behovløser.rettTilOrdinæreDagpenger(ident, søknadId) shouldBe true
    }

    @Test
    fun `Behovløser setter løsning til false når ingen arbeidsforhold har en ordinær sluttårsak`() {
        val svarMedIkkeOrdinærRettighetstype =
            listOf(
                ArbeidsforholdSvar(
                    navn = "arbeidsforhold",
                    land = "NOR",
                    sluttårsak = Sluttårsak.ARBEIDSGIVER_KONKURS,
                ),
                ArbeidsforholdSvar(
                    navn = "arbeidsforhold",
                    land = "NOR",
                    sluttårsak = Sluttårsak.PERMITTERT,
                ),
            )

        opplysningRepository.lagre(opplysning(svar = svarMedIkkeOrdinærRettighetstype))
        behovløser.rettTilOrdinæreDagpenger(ident, søknadId) shouldBe false
    }

    @Test
    fun `Behovløser setter løsning til false når det ikke er noen opplysning om arbeidsforhold`() {
        every {
            seksjonRepository.hentSeksjonsvarEllerKastException(
                any(),
                any(),
                any(),
            )
        } throws IllegalStateException("Fant ingen seksjonsvar på arbeidsforhold for søknad=$søknadId")
        behovløser.rettTilOrdinæreDagpenger(ident, søknadId) shouldBe false
    }

    private fun opplysning(
        svar: List<ArbeidsforholdSvar> =
            listOf(
                ArbeidsforholdSvar(
                    navn = "arbeidsforhold",
                    land = "NOR",
                    sluttårsak = Sluttårsak.KONTRAKT_UTGAATT,
                ),
            ),
    ): QuizOpplysning<List<ArbeidsforholdSvar>> {
        val opplysning =
            QuizOpplysning(
                beskrivendeId = behovløser.beskrivendeId,
                type = Arbeidsforhold,
                svar = svar,
                ident = ident,
                søknadId = søknadId,
            )
        return opplysning
    }
}
