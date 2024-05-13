package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.PermittertFiskeforedling
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysning
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.Arbeidsforhold
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.ArbeidsforholdSvar
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.Sluttårsak
import no.nav.dagpenger.soknad.orkestrator.utils.InMemoryOpplysningRepository
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import java.util.UUID
import kotlin.test.Test

class PermittertFiskeforedlingBehovløserTest {
    val opplysningRepository = InMemoryOpplysningRepository()
    val testRapid = TestRapid()
    val behovløser = PermittertFiskeforedlingBehovløser(testRapid, opplysningRepository)
    val ident = "12345678910"
    val søknadId = UUID.randomUUID()

    @Test
    fun `Behovløser publiserer løsning på behov PermittertFiskeforedling`() {
        opplysningRepository.lagre(opplysning())
        behovløser.løs(lagBehovmelding(ident, søknadId, PermittertFiskeforedling))

        testRapid.inspektør.message(0)["@løsning"]["PermittertFiskeforedling"]["verdi"].asBoolean() shouldBe true
    }

    @Test
    fun `Behovløser setter løsning til true når minst 1 arbeidsforhold har sluttårsak PERMITTERT_FISKEFOREDLING`() {
        val svarMedEttPermittertFiskeforedlingArbeidsforhold =
            listOf(
                ArbeidsforholdSvar(
                    navn = "arbeidsforhold1",
                    land = "NOR",
                    sluttårsak = Sluttårsak.PERMITTERT_FISKEFOREDLING,
                ),
                ArbeidsforholdSvar(
                    navn = "arbeidsforhold2",
                    land = "NOR",
                    sluttårsak = Sluttårsak.KONTRAKT_UTGAATT,
                ),
            )

        opplysningRepository.lagre(opplysning(svar = svarMedEttPermittertFiskeforedlingArbeidsforhold))
        behovløser.rettTilDagpengerUnderPermitteringFraFiskeindustri(ident, søknadId) shouldBe true
    }

    @Test
    fun `Behovløser setter løsning til false når ingen arbeidsforhold har sluttårsak PERMITTERT_FISKEFOREDLING`() {
        val svarUtenPermittertFiskeforedlingArbeidsforhold =
            listOf(
                ArbeidsforholdSvar(
                    navn = "arbeidsforhold",
                    land = "NOR",
                    sluttårsak = Sluttårsak.PERMITTERT,
                ),
            )

        opplysningRepository.lagre(opplysning(svar = svarUtenPermittertFiskeforedlingArbeidsforhold))
        behovløser.rettTilDagpengerUnderPermitteringFraFiskeindustri(ident, søknadId) shouldBe false
    }

    @Test
    fun `Behovløser setter løsning til false når det ikke er noen opplysning om arbeidsforhold`() {
        behovløser.rettTilDagpengerUnderPermitteringFraFiskeindustri(ident, søknadId) shouldBe false
    }

    private fun opplysning(
        svar: List<ArbeidsforholdSvar> =
            listOf(
                ArbeidsforholdSvar(
                    navn = "arbeidsforhold",
                    land = "NOR",
                    sluttårsak = Sluttårsak.PERMITTERT_FISKEFOREDLING,
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
