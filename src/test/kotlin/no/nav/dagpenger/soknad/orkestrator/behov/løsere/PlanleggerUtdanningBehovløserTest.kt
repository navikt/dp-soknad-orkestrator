package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.PlanleggerUtdanning
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.QuizOpplysning
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Boolsk
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Tekst
import no.nav.dagpenger.soknad.orkestrator.søknad.Søknad
import no.nav.dagpenger.soknad.orkestrator.søknad.Tilstand
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonRepository
import no.nav.dagpenger.soknad.orkestrator.utils.InMemoryQuizOpplysningRepository
import java.time.ZonedDateTime
import java.util.UUID.randomUUID
import kotlin.test.Test

class PlanleggerUtdanningBehovløserTest {
    val opplysningRepository = InMemoryQuizOpplysningRepository()
    val søknadRepository = mockk<SøknadRepository>(relaxed = true)
    val seksjonRepository = mockk<SeksjonRepository>(relaxed = true)
    val testRapid = TestRapid()
    val behovløser = PlanleggerUtdanningBehovløser(testRapid, opplysningRepository, søknadRepository, seksjonRepository)
    val ident = "12345678910"
    val søknadId = randomUUID()

    @Test
    fun `skal publisere løsning fra quiz-opplysning`() {
        val søknadstidspunkt = ZonedDateTime.now()
        opplysningRepository.lagre(
            QuizOpplysning(
                beskrivendeId = behovløser.beskrivendeId,
                type = Boolsk,
                svar = true,
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

        behovløser.løs(lagBehovmelding(ident, søknadId, PlanleggerUtdanning))

        testRapid.inspektør.message(0)["@løsning"][PlanleggerUtdanning.name].also { løsning ->
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
              "seksjonId": "utdanning",
              "seksjonsvar": {
                "planleggerÅStarteEllerFullføreStudierSamtidig": "ja"
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

        behovløser.løs(lagBehovmelding(ident, søknadId, PlanleggerUtdanning))

        verify { seksjonRepository.hentSeksjonsvarEllerKastException(ident, søknadId, "utdanning") }
        testRapid.inspektør.message(0)["@løsning"][PlanleggerUtdanning.name].also { løsning ->
            løsning["verdi"].asBoolean() shouldBe true
            løsning["gjelderFra"].asLocalDate() shouldBe søknadstidspunkt.toLocalDate()
        }
    }

    @Test
    fun `skal kaste feil dersom seksjon mangler`() {
        every {
            seksjonRepository.hentSeksjonsvarEllerKastException(any(), any(), any())
        } throws IllegalStateException("Fant ingen seksjonsvar på utdanning for søknad=$søknadId")

        shouldThrow<IllegalStateException> {
            behovløser.løs(lagBehovmelding(ident, søknadId, PlanleggerUtdanning))
        }
    }
}
