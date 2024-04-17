package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysning
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.Arbeidsforhold
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.ArbeidsforholdSvar
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.Sluttårsak
import no.nav.dagpenger.soknad.orkestrator.utils.InMemoryOpplysningRepository
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import java.util.UUID
import kotlin.test.Test

class JobbetUtenforNorgeBehovløserTest {
    val opplysningRepository = InMemoryOpplysningRepository()
    val testRapid = TestRapid()
    val behovløser = JobbetUtenforNorgeBehovløser(testRapid, opplysningRepository)
    private val ident = "12345678910"
    private val søknadId = UUID.randomUUID()

    @Test
    fun `Behovløser publiserer løsning på behov JobbetUtenforNorge`() {
        opplysningRepository.lagre(opplysning())
        behovløser.løs(lagBehovMelding(ident, søknadId, BehovløserFactory.Behov.JobbetUtenforNorge))

        testRapid.inspektør.message(0)["@løsning"]["JobbetUtenforNorge"]["verdi"].asText() shouldBe "false"
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
    ): Opplysning<List<ArbeidsforholdSvar>> {
        val opplysning =
            Opplysning(
                beskrivendeId = behovløser.beskrivendeId,
                type = Arbeidsforhold,
                svar = svar,
                ident = ident,
                søknadId = søknadId,
            )
        return opplysning
    }
}
