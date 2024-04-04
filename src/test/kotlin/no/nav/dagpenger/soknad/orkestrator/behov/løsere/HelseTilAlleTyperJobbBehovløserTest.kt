package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysning
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.Tekst
import no.nav.dagpenger.soknad.orkestrator.utils.InMemoryOpplysningRepository
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import java.util.UUID
import kotlin.test.Test

class HelseTilAlleTyperJobbBehovløserTest {
    val opplysningRepository = InMemoryOpplysningRepository()
    val testRapid = TestRapid()

    @Test
    fun `Behovløser publiserer løsning på behov HelseTilAlleTyperJobb`() {
        val ident = "12345678910"
        val søknadId = UUID.randomUUID()
        val svar = "true"

        val opplysning =
            Opplysning(
                beskrivendeId = "faktum.alle-typer-arbeid",
                type = Tekst,
                svar = svar,
                ident = ident,
                søknadId = søknadId,
            )

        opplysningRepository.lagre(opplysning)
        val behovløser = HelseTilAlleTyperJobbBehovløser(testRapid, opplysningRepository)
        behovløser.løs(ident, søknadId)

        testRapid.inspektør.message(0)["@løsning"]["HelseTilAlleTyperJobb"]["verdi"].asText() shouldBe svar
    }
}
