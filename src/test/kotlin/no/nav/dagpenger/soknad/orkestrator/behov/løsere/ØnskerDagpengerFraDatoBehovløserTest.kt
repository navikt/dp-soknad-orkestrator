package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysning
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.Dato
import no.nav.dagpenger.soknad.orkestrator.utils.InMemoryOpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.utils.januar
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import java.util.UUID
import kotlin.test.Test

class ØnskerDagpengerFraDatoBehovløserTest {
    val opplysningRepository = InMemoryOpplysningRepository()
    val testRapid = TestRapid()
    val behovløser = ØnskerDagpengerFraDatoBehovløser(testRapid, opplysningRepository)
    val ident = "12345678910"
    val søknadId = UUID.randomUUID()

    @Test
    fun `Behovløser publiserer løsning på behov ØnskerDagpengerFraDato`() {
        val svar = 1.januar(2021)

        val opplysning =
            Opplysning(
                beskrivendeId = behovløser.beskrivendeId,
                type = Dato,
                svar = svar,
                ident = ident,
                søknadId = søknadId,
            )

        opplysningRepository.lagre(opplysning)
        behovløser.løs(lagBehovMelding(ident, søknadId, BehovløserFactory.Behov.ØnskerDagpengerFraDato))

        testRapid.inspektør.message(0)["@løsning"]["ØnskerDagpengerFraDato"]["verdi"].asLocalDate() shouldBe svar
    }

    @Test
    fun `Behovløser kaster feil dersom det ikke finnes en opplysning som kan besvare behovet`() {
        shouldThrow<IllegalStateException> {
            behovløser.løs(
                lagBehovMelding(ident, søknadId, BehovløserFactory.Behov.ØnskerDagpengerFraDato),
            )
        }
    }
}
