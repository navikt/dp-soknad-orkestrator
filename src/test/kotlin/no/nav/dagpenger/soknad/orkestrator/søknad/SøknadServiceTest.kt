package no.nav.dagpenger.soknad.orkestrator.søknad

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysning
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.Tekst
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test
import java.util.UUID

class SøknadServiceTest {
    private val testRapid = TestRapid()
    private var søknadService = SøknadService(testRapid)

    @Test
    fun `vi kan sende ut melding om ny søknad på rapiden`() {
        val ident = "12345678901"
        val søknadUUID = UUID.randomUUID()
        val søknad =
            Søknad(
                id = søknadUUID,
                ident = ident,
                opplysninger =
                    listOf(
                        Opplysning(
                            beskrivendeId = "faktum.hvilket-land-bor-du-i",
                            type = Tekst,
                            svar = "NOR",
                            ident = ident,
                            søknadId = søknadUUID,
                        ),
                    ),
            )

        søknadService.publiserMeldingOmNySøknad(søknad)

        with(testRapid.inspektør) {
            size shouldBe 1
            field(0, "@event_name").asText() shouldBe "melding_om_ny_søknad"
            field(0, "søknad_uuid").asText() shouldBe søknadUUID.toString()
            field(0, "ident").asText() shouldBe ident
        }
    }
}
