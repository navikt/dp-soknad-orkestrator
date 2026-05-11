package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.EØSPengestøtte
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.EØSPengestøtteBehovløser.Companion.EØS_DAGPENGER_SVAR
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.QuizOpplysning
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Flervalg
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Tekst
import no.nav.dagpenger.soknad.orkestrator.søknad.Søknad
import no.nav.dagpenger.soknad.orkestrator.søknad.Tilstand
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonRepository
import no.nav.dagpenger.soknad.orkestrator.utils.InMemoryQuizOpplysningRepository
import java.time.ZonedDateTime
import java.util.UUID.randomUUID
import kotlin.test.Test

class EØSPengestøtteBehovløserTest {
    val opplysningRepository = InMemoryQuizOpplysningRepository()
    val søknadRepository = mockk<SøknadRepository>(relaxed = true)
    val seksjonRepository = mockk<SeksjonRepository>(relaxed = true)
    val testRapid = TestRapid()
    val behovløser = EØSPengestøtteBehovløser(testRapid, opplysningRepository, søknadRepository, seksjonRepository)
    val ident = "12345678910"
    val søknadId = randomUUID()

    @Test
    fun `skal returnere true når EØS-dagpenger-svar finnes i quiz-flervalg`() {
        opplysningRepository.lagre(
            QuizOpplysning(
                beskrivendeId = behovløser.beskrivendeId,
                type = Flervalg,
                svar = listOf(EØS_DAGPENGER_SVAR, "faktum.hvilke-andre-ytelser.svar.noe-annet"),
                ident = ident,
                søknadId = søknadId,
            ),
        )

        behovløser.harEøsPengestøtte(ident, søknadId) shouldBe true
    }

    @Test
    fun `skal returnere false når EØS-dagpenger-svar ikke finnes i quiz-flervalg`() {
        opplysningRepository.lagre(
            QuizOpplysning(
                beskrivendeId = behovløser.beskrivendeId,
                type = Flervalg,
                svar = listOf("faktum.hvilke-andre-ytelser.svar.noe-annet"),
                ident = ident,
                søknadId = søknadId,
            ),
        )

        behovløser.harEøsPengestøtte(ident, søknadId) shouldBe false
    }

    @Test
    fun `skal publisere løsning fra quiz-opplysning med gjelderFra`() {
        val søknadstidspunkt = ZonedDateTime.now()
        opplysningRepository.lagre(
            QuizOpplysning(
                beskrivendeId = behovløser.beskrivendeId,
                type = Flervalg,
                svar = listOf(EØS_DAGPENGER_SVAR),
                ident = ident,
                søknadId = søknadId,
            ),
        )
        opplysningRepository.lagre(
            QuizOpplysning(
                beskrivendeId = "søknadstidspunkt",
                type = Tekst,
                svar = søknadstidspunkt.toString(),
                ident = ident,
                søknadId = søknadId,
            ),
        )

        behovløser.løs(lagBehovmelding(ident, søknadId, EØSPengestøtte))

        testRapid.inspektør.message(0)["@løsning"][EØSPengestøtte.name].also { løsning ->
            løsning["verdi"].asBoolean() shouldBe true
            løsning["gjelderFra"].asLocalDate() shouldBe søknadstidspunkt.toLocalDate()
        }
    }

    @Test
    fun `skal publisere løsning fra seksjonsdata`() {
        val søknadstidspunkt = ZonedDateTime.now()
        every { seksjonRepository.hentSeksjonsvarEllerKastException(any(), any(), any()) } returns
            """
            {
              "seksjonId": "annen-pengestøtte",
              "seksjonsvar": {
                "harMottattEllerSøktOmPengestøtteFraAndreEøsLand": "ja"
              },
              "versjon": 1
            }
            """.trimIndent()
        every { søknadRepository.hent(any()) } returns
            Søknad(
                søknadId = søknadId,
                ident = ident,
                tilstand = Tilstand.INNSENDT,
                innsendtTidspunkt = søknadstidspunkt.toLocalDateTime(),
            )

        behovløser.løs(lagBehovmelding(ident, søknadId, EØSPengestøtte))

        verify { seksjonRepository.hentSeksjonsvarEllerKastException(ident, søknadId, "annen-pengestøtte") }
        testRapid.inspektør.message(0)["@løsning"][EØSPengestøtte.name].also { løsning ->
            løsning["verdi"].asBoolean() shouldBe true
            løsning["gjelderFra"].asLocalDate() shouldBe søknadstidspunkt.toLocalDate()
        }
    }

    @Test
    fun `skal returnere false når quiz-flervalg er tom liste`() {
        opplysningRepository.lagre(
            QuizOpplysning(
                beskrivendeId = behovløser.beskrivendeId,
                type = Flervalg,
                svar = emptyList<String>(),
                ident = ident,
                søknadId = søknadId,
            ),
        )

        behovløser.harEøsPengestøtte(ident, søknadId) shouldBe false
    }

    @Test
    fun `skal returnere false når seksjonsdata har nei`() {
        every { seksjonRepository.hentSeksjonsvarEllerKastException(any(), any(), any()) } returns
            """
            {
              "seksjonId": "annen-pengestøtte",
              "seksjonsvar": {
                "harMottattEllerSøktOmPengestøtteFraAndreEøsLand": "nei"
              },
              "versjon": 1
            }
            """.trimIndent()

        behovløser.harEøsPengestøtte(ident, søknadId) shouldBe false
    }

    @Test
    fun `skal returnere false når feltet mangler i seksjonsdata`() {
        every { seksjonRepository.hentSeksjonsvarEllerKastException(any(), any(), any()) } returns
            """
            {
              "seksjonId": "annen-pengestøtte",
              "seksjonsvar": {},
              "versjon": 1
            }
            """.trimIndent()

        behovløser.harEøsPengestøtte(ident, søknadId) shouldBe false
    }

    @Test
    fun `skal kaste feil dersom seksjon mangler`() {
        every {
            seksjonRepository.hentSeksjonsvarEllerKastException(any(), any(), any())
        } throws IllegalStateException("Fant ingen seksjonsvar på annen-pengestøtte for søknad=$søknadId")

        shouldThrow<IllegalStateException> {
            behovløser.løs(lagBehovmelding(ident, søknadId, EØSPengestøtte))
        }
    }
}
