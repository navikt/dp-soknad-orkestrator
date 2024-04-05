package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysning
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.Tekst
import no.nav.dagpenger.soknad.orkestrator.utils.InMemoryOpplysningRepository
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import java.util.UUID
import kotlin.test.Test

class VilligTilÅBytteYrkeBehovløserTest {
    val opplysningRepository = InMemoryOpplysningRepository()
    val testRapid = TestRapid()
    val behovløser = VilligTilÅBytteYrkeBehovløser(testRapid, opplysningRepository)
    val ident = "12345678910"
    val søknadId = UUID.randomUUID()

    @Test
    fun `Behovløser publiserer løsning på behov VilligTilÅBytteYrke`() {
        val opplysning =
            Opplysning(
                beskrivendeId = "faktum.bytte-yrke-ned-i-lonn",
                type = Tekst,
                svar = "true",
                ident = ident,
                søknadId = søknadId,
            )

        opplysningRepository.lagre(opplysning)
        behovløser.løs(ident, søknadId)

        testRapid.inspektør.message(0)["@løsning"]["VilligTilÅBytteYrke"]["verdi"].asText() shouldBe "true"
    }

    @Test
    fun `Behovløser kaster feil dersom det ikke finnes en opplysning som kan besvare behovet`() {
        shouldThrow<IllegalStateException> { behovløser.løs(ident, søknadId) }
    }
}
