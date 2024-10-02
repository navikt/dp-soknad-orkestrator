package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.QuizOpplysning
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Arbeidsforhold
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.ArbeidsforholdSvar
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Sluttårsak
import no.nav.dagpenger.soknad.orkestrator.utils.InMemoryQuizOpplysningRepository
import java.util.UUID
import kotlin.test.Test

class JobbetUtenforNorgeBehovløserTest {
    val opplysningRepository = InMemoryQuizOpplysningRepository()
    val testRapid = TestRapid()
    val behovløser = JobbetUtenforNorgeBehovløser(testRapid, opplysningRepository)
    private val ident = "12345678910"
    private val søknadId = UUID.randomUUID()

    @Test
    fun `Behovløser publiserer løsning på behov JobbetUtenforNorge`() {
        opplysningRepository.lagre(opplysning())
        behovløser.løs(lagBehovmelding(ident, søknadId, BehovløserFactory.Behov.JobbetUtenforNorge))

        testRapid.inspektør.message(0)["@løsning"]["JobbetUtenforNorge"]["verdi"].asBoolean() shouldBe false
    }

    @Test
    fun `Behovløser setter løsning til true når det er jobbet utenfor Norge`() {
        val svarMedArbeidUtenforNorge =
            listOf(
                ArbeidsforholdSvar(
                    navn = "arbeidsforhold1",
                    land = "NOR",
                    sluttårsak = Sluttårsak.KONTRAKT_UTGAATT,
                ),
                ArbeidsforholdSvar(
                    navn = "arbeidsforhold2",
                    land = "SWE",
                    sluttårsak = Sluttårsak.KONTRAKT_UTGAATT,
                ),
            )

        opplysningRepository.lagre(opplysning(svar = svarMedArbeidUtenforNorge))
        behovløser.harJobbetUtenforNorge(ident, søknadId) shouldBe true
    }

    @Test
    fun `Behovløser setter løsning til false når det ikke er jobbet utenfor Norge`() {
        val svarMedArbeidINorge =
            listOf(
                ArbeidsforholdSvar(
                    navn = "arbeidsforhold1",
                    land = "NOR",
                    sluttårsak = Sluttårsak.KONTRAKT_UTGAATT,
                ),
            )

        opplysningRepository.lagre(opplysning(svar = svarMedArbeidINorge))
        behovløser.harJobbetUtenforNorge(ident, søknadId) shouldBe false
    }

    @Test
    fun `Behovløser setter løsning til false når det ikke er noen opplysning om arbeidsforhold`() {
        behovløser.harJobbetUtenforNorge(ident, søknadId) shouldBe false
    }

    private fun opplysning(
        svar: List<ArbeidsforholdSvar> =
            listOf(
                ArbeidsforholdSvar(
                    navn = "arbeidsforhold1",
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
