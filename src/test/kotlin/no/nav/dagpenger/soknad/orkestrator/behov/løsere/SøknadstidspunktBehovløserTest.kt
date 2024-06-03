package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysning
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.Tekst
import no.nav.dagpenger.soknad.orkestrator.utils.InMemoryOpplysningRepository
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
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
        val søknadsTidspunkt = ZonedDateTime.parse("2024-01-01T10:15:30+02:00")
        val formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
        val formatertTidspunkt = søknadsTidspunkt.format(formatter)

        val opplysning =
            Opplysning(
                beskrivendeId = behovløser.beskrivendeId,
                type = Tekst,
                svar = formatertTidspunkt,
                ident = ident,
                søknadId = søknadId,
            )

        opplysningRepository.lagre(opplysning)
        behovløser.løs(lagBehovmelding(ident, søknadId, BehovløserFactory.Behov.Søknadstidspunkt))

        testRapid.inspektør.message(0)["@løsning"]["Søknadstidspunkt"]["verdi"].asText() shouldBe "2024-01-01"
    }

    @Test
    fun `Behovløser kaster feil dersom det ikke finnes en opplysning som kan besvare behovet`() {
        shouldThrow<IllegalStateException> { behovløser.løs(lagBehovmelding(ident, søknadId, BehovløserFactory.Behov.Søknadstidspunkt)) }
    }
}
