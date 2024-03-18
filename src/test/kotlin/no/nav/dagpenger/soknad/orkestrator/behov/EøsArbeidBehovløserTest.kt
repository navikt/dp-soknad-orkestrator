package no.nav.dagpenger.soknad.orkestrator.behov

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysning
import no.nav.dagpenger.soknad.orkestrator.utils.InMemoryOpplysningRepository
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import java.util.UUID
import kotlin.test.Test

class EøsArbeidBehovløserTest {
    val opplysningRepository = InMemoryOpplysningRepository()
    val testRapid = TestRapid()

    @Test
    fun `Behovløser publiserer løsning på behov EøsArbeid`() {
        val ident = "12345678910"
        val søknadsId = UUID.randomUUID()
        val svar = "false"

        val opplysning =
            Opplysning(
                beskrivendeId = "eos-arbeid-siste-36-mnd",
                ident = ident,
                søknadsId = søknadsId,
                svar = svar,
            )

        opplysningRepository.lagre(opplysning)
        val behovløser = EøsArbeidBehovløser(testRapid, opplysningRepository)
        behovløser.løs(ident, søknadsId)

        testRapid.inspektør.message(0)["@løsning"]["EøsArbeid"]["verdi"].asText() shouldBe svar
    }
}
