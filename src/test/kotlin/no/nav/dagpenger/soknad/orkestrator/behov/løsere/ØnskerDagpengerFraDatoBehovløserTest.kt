package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.QuizOpplysning
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Dato
import no.nav.dagpenger.soknad.orkestrator.utils.InMemoryQuizOpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.utils.januar
import java.util.UUID
import kotlin.test.Test

class ØnskerDagpengerFraDatoBehovløserTest {
    val opplysningRepository = InMemoryQuizOpplysningRepository()
    val testRapid = TestRapid()
    val behovløser = ØnskerDagpengerFraDatoBehovløser(testRapid, opplysningRepository)
    val ident = "12345678910"
    val søknadId = UUID.randomUUID()

    @Test
    fun `Behovløser publiserer løsning på behov ØnskerDagpengerFraDato`() {
        val svar = 1.januar(2021)

        val opplysning =
            QuizOpplysning(
                beskrivendeId = behovløser.beskrivendeId,
                type = Dato,
                svar = svar,
                ident = ident,
                søknadId = søknadId,
            )

        opplysningRepository.lagre(opplysning)
        behovløser.løs(lagBehovmelding(ident, søknadId, BehovløserFactory.Behov.ØnskerDagpengerFraDato))

        testRapid.inspektør.message(0)["@løsning"]["ØnskerDagpengerFraDato"]["verdi"].asLocalDate() shouldBe svar
    }

    @Test
    fun `Behovløser kaster feil dersom det ikke finnes en opplysning som kan besvare behovet`() {
        shouldThrow<IllegalStateException> {
            behovløser.løs(
                lagBehovmelding(ident, søknadId, BehovløserFactory.Behov.ØnskerDagpengerFraDato),
            )
        }
    }
}
