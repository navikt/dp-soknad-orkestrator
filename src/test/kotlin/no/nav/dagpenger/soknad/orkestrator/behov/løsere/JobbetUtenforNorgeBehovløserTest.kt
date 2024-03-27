package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysning
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.Arbeidsforhold
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.ArbeidsforholdSvar
import no.nav.dagpenger.soknad.orkestrator.utils.InMemoryOpplysningRepository
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import java.util.UUID
import kotlin.test.Test

class JobbetUtenforNorgeBehovløserTest {
    val opplysningRepository = InMemoryOpplysningRepository()
    val testRapid = TestRapid()
    private val ident = "12345678910"
    private val søknadsId = UUID.randomUUID()

    @Test
    fun `Behovløser publiserer løsning på behov JobbetUtenforNorge`() {
        opplysningRepository.lagre(opplysning())
        val behovløser = JobbetUtenforNorgeBehovløser(testRapid, opplysningRepository)
        behovløser.løs(ident, søknadsId)

        testRapid.inspektør.message(0)["@løsning"]["JobbetUtenforNorge"]["verdi"].asText() shouldBe "false"
    }

    @Test
    fun `Finner riktig løsning når det er jobbet utenfor Norge`() {
        val svarMedArbeidUtenforNorge =
            listOf(
                ArbeidsforholdSvar(navn = "arbeidsforhold1", land = "NOR"),
                ArbeidsforholdSvar(navn = "arbeidsforhold2", land = "SWE"),
            )

        opplysningRepository.lagre(opplysning(svar = svarMedArbeidUtenforNorge))
        val behovløser = JobbetUtenforNorgeBehovløser(testRapid, opplysningRepository)
        behovløser.harJobbetUtenforNorge(ident, søknadsId) shouldBe true
    }

    @Test
    fun `Finner riktig løsning når det ikke er jobbet utenfor Norge`() {
        val svarMedArbeidINorge =
            listOf(
                ArbeidsforholdSvar(navn = "arbeidsforhold1", land = "NOR"),
            )

        opplysningRepository.lagre(opplysning(svar = svarMedArbeidINorge))
        val behovløser = JobbetUtenforNorgeBehovløser(testRapid, opplysningRepository)
        behovløser.harJobbetUtenforNorge(ident, søknadsId) shouldBe false
    }

    private fun opplysning(
        svar: List<ArbeidsforholdSvar> =
            listOf(
                ArbeidsforholdSvar(
                    navn = "arbeidsforhold1",
                    land = "NOR",
                ),
            ),
    ): Opplysning<List<ArbeidsforholdSvar>> {
        val opplysning =
            Opplysning(
                beskrivendeId = "faktum.arbeidsforhold",
                type = Arbeidsforhold,
                svar = svar,
                ident = ident,
                søknadsId = søknadsId,
            )
        return opplysning
    }
}
