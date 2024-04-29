package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysning
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.Tekst
import no.nav.dagpenger.soknad.orkestrator.utils.InMemoryOpplysningRepository
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Ignore
import kotlin.test.Test

class SøknadsdatoBehovløserTest {
    val opplysningRepository = InMemoryOpplysningRepository()
    val testRapid = TestRapid()
    val behovløser = SøknadsdatoBehovløser(testRapid, opplysningRepository)
    val ident = "12345678910"
    val søknadId = UUID.randomUUID()

    @Test
    @Ignore
    fun `Behovløser publiserer løsning på behov Søknadsdato`() {
        val opplysning =
            Opplysning(
                beskrivendeId = behovløser.beskrivendeId,
                type = Tekst,
                svar = "2024-04-17T12:04:20+02:00",
                ident = ident,
                søknadId = søknadId,
            )

        opplysningRepository.lagre(opplysning)
        behovløser.løs(lagBehovmelding(ident, søknadId, BehovløserFactory.Behov.Søknadsdato))

        testRapid.inspektør.message(0)["@løsning"]["Søknadsdato"]["verdi"].asLocalDate() shouldBe LocalDate.parse("2024-04-17")
    }

    @Test
    fun `Behovløser kaster feil dersom det ikke finnes en opplysning som kan besvare behovet`() {
        shouldThrow<IllegalStateException> { behovløser.løs(lagBehovmelding(ident, søknadId, BehovløserFactory.Behov.Søknadsdato)) }
    }
}
