package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.Permittert
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysning
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.Arbeidsforhold
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.ArbeidsforholdSvar
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.Sluttårsak
import no.nav.dagpenger.soknad.orkestrator.utils.InMemoryOpplysningRepository
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import java.util.UUID
import kotlin.test.Ignore
import kotlin.test.Test

class PermittertBehovløserTest {
    val opplysningRepository = InMemoryOpplysningRepository()
    val testRapid = TestRapid()
    val behovløser = PermittertBehovløser(testRapid, opplysningRepository)
    val ident = "12345678910"
    val søknadId = UUID.randomUUID()

    @Test
    @Ignore
    fun `Behovløser publiserer løsning på behov Permittert`() {
        opplysningRepository.lagre(opplysning())
        behovløser.løs(lagBehovmelding(ident, søknadId, Permittert))

        testRapid.inspektør.message(0)["@løsning"]["Permittert"]["verdi"].asBoolean() shouldBe true
    }

    @Test
    fun `Behovløser setter løsning til true når minst ett arbeidsforhold har sluttårsak PERMITTERT`() {
        val svarMedEttPermittertArbeidsforhold =
            listOf(
                ArbeidsforholdSvar(
                    navn = "arbeidsforhold1",
                    land = "NOR",
                    sluttårsak = Sluttårsak.PERMITTERT,
                ),
                ArbeidsforholdSvar(
                    navn = "arbeidsforhold2",
                    land = "NOR",
                    sluttårsak = Sluttårsak.KONTRAKT_UTGAATT,
                ),
            )

        opplysningRepository.lagre(opplysning(svar = svarMedEttPermittertArbeidsforhold))
        behovløser.rettTilDagpengerUnderPermittering(ident, søknadId) shouldBe true
    }

    @Test
    fun `Behovløser setter løsning til false når ingen arbeidsforhold har sluttårsak PERMITTERT`() {
        val svarUtenPermittertArbeidsforhold =
            listOf(
                ArbeidsforholdSvar(
                    navn = "arbeidsforhold",
                    land = "NOR",
                    sluttårsak = Sluttårsak.ARBEIDSGIVER_KONKURS,
                ),
            )

        opplysningRepository.lagre(opplysning(svar = svarUtenPermittertArbeidsforhold))
        behovløser.rettTilDagpengerUnderPermittering(ident, søknadId) shouldBe false
    }

    @Test
    fun `Behovløser setter løsning til false når det ikke er noen opplysning om arbeidsforhold`() {
        behovløser.rettTilDagpengerUnderPermittering(ident, søknadId) shouldBe false
    }

    private fun opplysning(
        svar: List<ArbeidsforholdSvar> =
            listOf(
                ArbeidsforholdSvar(
                    navn = "arbeidsforhold",
                    land = "NOR",
                    sluttårsak = Sluttårsak.PERMITTERT,
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
