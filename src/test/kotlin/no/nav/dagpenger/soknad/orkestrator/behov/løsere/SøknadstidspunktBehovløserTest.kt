package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysning
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.Tekst
import no.nav.dagpenger.soknad.orkestrator.utils.InMemoryOpplysningRepository
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import java.util.UUID
import kotlin.test.Test

class SøknadstidspunktBehovløserTest {
    val opplysningRepository = InMemoryOpplysningRepository()
    val testRapid = TestRapid()
    val behovløser = SøknadstidspunktBehovløser(testRapid, opplysningRepository)
    val ident = "12345678910"
    val søknadId = UUID.randomUUID()

    @Test
    fun `Behovløser publiserer løsning på behov Søknadstidspunkt`() {
        val opplysning =
            Opplysning(
                beskrivendeId = "søknadstidspunkt",
                type = Tekst,
                svar = "false",
                ident = ident,
                søknadId = søknadId,
            )

        opplysningRepository.lagre(opplysning)
        behovløser.løs(ident, søknadId)

        testRapid.inspektør.message(0)["@løsning"]["Søknadstidspunkt"]["verdi"].asText() shouldBe "false"
    }

    @Test
    fun `Behovløser kaster feil dersom det ikke finnes en opplysning som kan besvare behovet`() {
        shouldThrow<IllegalStateException> { behovløser.løs(ident, søknadId) }
    }
}
