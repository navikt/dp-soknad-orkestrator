package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.OppgittAndreYtelserUtenforNav
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysning
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.Boolsk
import no.nav.dagpenger.soknad.orkestrator.utils.InMemoryOpplysningRepository
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import java.util.UUID
import kotlin.test.Test

class OppgittAndreYtelserUtenforNavBehovløserTest {
    val opplysningRepository = InMemoryOpplysningRepository()
    val testRapid = TestRapid()
    val behovløser = OppgittAndreYtelserUtenforNavBehovløser(testRapid, opplysningRepository)
    val ident = "12345678910"
    val søknadId = UUID.randomUUID()

    @Test
    fun `Behovløser publiserer løsning på behov OppgittAndreYtelserUtenforNav`() {
        val opplysning =
            Opplysning(
                beskrivendeId = behovløser.beskrivendeId,
                type = Boolsk,
                svar = false,
                ident = ident,
                søknadId = søknadId,
            )

        opplysningRepository.lagre(opplysning)
        behovløser.løs(
            lagBehovmelding(
                ident,
                søknadId,
                OppgittAndreYtelserUtenforNav,
            ),
        )

        testRapid.inspektør.message(0)["@løsning"]["OppgittAndreYtelserUtenforNav"]["verdi"].asBoolean() shouldBe false
    }

    @Test
    fun `Behovløser kaster feil dersom det ikke finnes en opplysning som kan besvare behovet`() {
        shouldThrow<IllegalStateException> {
            behovløser.løs(
                lagBehovmelding(
                    ident,
                    søknadId,
                    OppgittAndreYtelserUtenforNav,
                ),
            )
        }
    }
}
