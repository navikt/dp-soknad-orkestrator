package no.nav.dagpenger.soknad.orkestrator.behov

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysning
import no.nav.dagpenger.soknad.orkestrator.utils.InMemoryOpplysningRepository
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import java.util.UUID
import kotlin.test.Test

class KanJobbeDeltidBehovløserTest {
    val opplysningRepository = InMemoryOpplysningRepository()
    val testRapid = TestRapid()

    @Test
    fun `Behovløser publiserer løsning på behov KanJobbeDeltid`() {
        val ident = "12345678910"
        val søknadsId = UUID.randomUUID()
        val behandlingsId = UUID.randomUUID()
        val svar = "2021-01-01"

        val opplysning =
            Opplysning(
                beskrivendeId = "jobbe-hel-deltid",
                ident = ident,
                søknadsId = søknadsId,
                behandlingsId = behandlingsId,
                svar = svar,
            )

        opplysningRepository.lagre(opplysning)
        val behovløser = KanJobbeDeltidBehovløser(testRapid, opplysningRepository)
        behovløser.løs(ident, søknadsId, behandlingsId)

        testRapid.inspektør.message(0)["@løsning"]["KanJobbeDeltid"]["verdi"].asText() shouldBe svar
    }
}
