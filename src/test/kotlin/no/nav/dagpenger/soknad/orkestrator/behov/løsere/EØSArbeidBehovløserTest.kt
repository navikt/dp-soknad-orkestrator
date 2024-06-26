package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysning
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.Boolsk
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.Tekst
import no.nav.dagpenger.soknad.orkestrator.utils.InMemoryOpplysningRepository
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import java.util.UUID
import kotlin.test.Test

class EØSArbeidBehovløserTest {
    val opplysningRepository = InMemoryOpplysningRepository()
    val testRapid = TestRapid()
    val behovløser = EØSArbeidBehovløser(testRapid, opplysningRepository)
    val ident = "12345678910"
    val søknadId = UUID.randomUUID()

    @Test
    fun `Behovløser publiserer løsning på behov EøsArbeid`() {
        val opplysning =
            Opplysning(
                beskrivendeId = behovløser.beskrivendeId,
                type = Boolsk,
                svar = true,
                ident = ident,
                søknadId = søknadId,
            )

        opplysningRepository.lagre(opplysning)
        behovløser.løs(lagBehovmelding(ident, søknadId, BehovløserFactory.Behov.EØSArbeid))

        testRapid.inspektør.message(0)["@løsning"]["EØSArbeid"]["verdi"].asBoolean() shouldBe true
    }

    @Test
    fun `Behovløser setter løsning til true når det er jobbet i eøs siste 36 mnd`() {
        val opplysning =
            Opplysning(
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
            Opplysning(
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
