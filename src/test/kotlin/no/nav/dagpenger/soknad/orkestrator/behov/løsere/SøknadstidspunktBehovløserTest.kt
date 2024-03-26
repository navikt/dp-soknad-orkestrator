package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysning
import no.nav.dagpenger.soknad.orkestrator.opplysning.Tekst
import no.nav.dagpenger.soknad.orkestrator.utils.InMemoryOpplysningRepository
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import java.util.UUID
import kotlin.test.Test

class SøknadstidspunktBehovløserTest {
    val opplysningRepository = InMemoryOpplysningRepository()
    val testRapid = TestRapid()

    @Test
    fun `Behovløser publiserer løsning på behov Søknadstidspunkt`() {
        val ident = "12345678910"
        val søknadsId = UUID.randomUUID()
        val svar = "false"

        val opplysning =
            Opplysning(
                beskrivendeId = "søknadstidspunkt",
                type = Tekst,
                svar = svar,
                ident = ident,
                søknadsId = søknadsId,
            )

        opplysningRepository.lagre(opplysning)
        val behovløser = SøknadstidspunktBehovløser(testRapid, opplysningRepository)
        behovløser.løs(ident, søknadsId)

        testRapid.inspektør.message(0)["@løsning"]["Søknadstidspunkt"]["verdi"].asText() shouldBe svar
    }
}
