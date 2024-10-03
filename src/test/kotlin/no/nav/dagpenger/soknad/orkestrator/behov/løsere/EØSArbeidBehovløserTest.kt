package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.QuizOpplysning
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Boolsk
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Tekst
import no.nav.dagpenger.soknad.orkestrator.utils.InMemoryQuizOpplysningRepository
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.test.Test

class EØSArbeidBehovløserTest {
    val opplysningRepository = InMemoryQuizOpplysningRepository()
    val testRapid = TestRapid()
    val behovløser = EØSArbeidBehovløser(testRapid, opplysningRepository)
    val ident = "12345678910"
    val søknadId = UUID.randomUUID()

    @Test
    fun `Behovløser publiserer løsning på behov EøsArbeid med verdi og gjelderFra`() {
        val opplysning =
            QuizOpplysning(
                beskrivendeId = behovløser.beskrivendeId,
                type = Boolsk,
                svar = true,
                ident = ident,
                søknadId = søknadId,
            )

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

        opplysningRepository.lagre(opplysning)
        opplysningRepository.lagre(søknadstidpsunktOpplysning)
        behovløser.løs(lagBehovmelding(ident, søknadId, BehovløserFactory.Behov.EØSArbeid))

        testRapid.inspektør.message(0)["@løsning"]["EØSArbeid"].also { løsning ->
            løsning["verdi"].asBoolean() shouldBe true
            løsning["gjelderFra"].asLocalDate() shouldBe søknadstidspunkt.toLocalDate()
        }
    }

    @Test
    fun `Behovløser setter løsning til true når det er jobbet i eøs siste 36 mnd`() {
        val opplysning =
            QuizOpplysning(
                beskrivendeId = "faktum.eos-arbeid-siste-36-mnd",
                type = Tekst,
                svar = "true",
                ident = ident,
                søknadId = søknadId,
            )

        opplysningRepository.lagre(opplysning)
        behovløser.løs(lagBehovmelding(ident, søknadId, BehovløserFactory.Behov.EØSArbeid))

        behovløser.harJobbetIEøsSiste36mnd(ident, søknadId) shouldBe "true"
    }

    @Test
    fun `Behovløser setter løsning til false når det ikke er jobbet i eøs siste 36 mnd`() {
        val opplysning =
            QuizOpplysning(
                beskrivendeId = "faktum.eos-arbeid-siste-36-mnd",
                type = Tekst,
                svar = "false",
                ident = ident,
                søknadId = søknadId,
            )

        opplysningRepository.lagre(opplysning)
        behovløser.løs(lagBehovmelding(ident, søknadId, BehovløserFactory.Behov.EØSArbeid))

        behovløser.harJobbetIEøsSiste36mnd(ident, søknadId) shouldBe "false"
    }

    @Test
    fun `Behovløser svarer false dersom opplysning om Eøs arbeid ikke finnes`() {
        val behovmelding = lagBehovmelding(ident, søknadId, BehovløserFactory.Behov.EØSArbeid)
        behovløser.løs(behovmelding)
        behovløser.harJobbetIEøsSiste36mnd(ident, søknadId) shouldBe false
    }
}
