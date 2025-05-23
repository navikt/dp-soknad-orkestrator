package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.EgenNæringsvirksomhet
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.QuizOpplysning
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Boolsk
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Tekst
import no.nav.dagpenger.soknad.orkestrator.utils.InMemoryQuizOpplysningRepository
import java.time.ZonedDateTime
import java.util.UUID.randomUUID
import kotlin.test.Test

class EgenNæringsvirksomhetBehovløserTest {
    val opplysningRepository = InMemoryQuizOpplysningRepository()
    val testRapid = TestRapid()
    val behovløser = EgenNæringsvirksomhetBehovløser(testRapid, opplysningRepository)
    val ident = "12345678910"
    val søknadId = randomUUID()

    @Test
    fun `Behovløser publiserer løsning på behov EgenNæringsvirksomhet med verdi og gjelderFra`() {
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
        behovløser.løs(lagBehovmelding(ident, søknadId, EgenNæringsvirksomhet))

        testRapid.inspektør.message(0)["@løsning"][EgenNæringsvirksomhet.name].also { løsning ->
            løsning["verdi"].asBoolean() shouldBe true
            løsning["gjelderFra"].asLocalDate() shouldBe søknadstidspunkt.toLocalDate()
        }
    }

    @Test
    fun `Behovløser kaster feil dersom det ikke finnes en opplysning som kan besvare behovet`() {
        shouldThrow<IllegalStateException> {
            behovløser.løs(
                lagBehovmelding(ident, søknadId, EgenNæringsvirksomhet),
            )
        }
    }
}
