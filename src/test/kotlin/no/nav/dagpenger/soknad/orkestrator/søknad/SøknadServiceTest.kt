package no.nav.dagpenger.soknad.orkestrator.søknad

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysning
import no.nav.dagpenger.soknad.orkestrator.utils.februar
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test
import java.util.UUID

class SøknadServiceTest {
    private val testRapid = TestRapid()
    private var søknadService = SøknadService(testRapid)

    @Test
    fun `vi kan mappe LegacySøknad til Søknad`() {
        val id = UUID.randomUUID()
        val søknadUUID = UUID.randomUUID()
        val journalpostId = "637582711"

        val legacySøknad =
            LegacySøknad(
                id = id,
                opprettet = 20.februar.atStartOfDay(),
                ident = "12345678901",
                journalpostId = journalpostId,
                søknadsData =
                    SøknadsData(
                        opprettet = 20.februar.atStartOfDay(),
                        søknadUUID = søknadUUID,
                        seksjoner =
                            listOf(
                                Seksjoner(
                                    beskrivendeId = "bostedsland",
                                    fakta =
                                        listOf(
                                            Fakta(
                                                beskrivendeId = "faktum.hvilket-land-bor-du-i",
                                                svar = listOf("NOR"),
                                                type = "land",
                                            ),
                                        ),
                                ),
                            ),
                    ),
            )

        val søknad = toSøknad(legacySøknad)

        with(søknad) {
            id shouldBe id
            journalpostId shouldBe journalpostId
            ident shouldBe "12345678901"
            opplysninger.size shouldBe 1
            opplysninger.first().beskrivendeId shouldBe "hvilket-land-bor-du-i"
            opplysninger.first().svar shouldBe listOf("NOR")
        }
    }

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
                            svar = listOf("NOR"),
                            søknadsId = søknadUUID,
                            ident = ident,
                        ),
                    ),
            )

        søknadService.publiserMeldingOmNySøknad(søknad)

        with(testRapid.inspektør) {
            size shouldBe 1
            field(0, "@event_name").asText() shouldBe "MeldingOmNySøknad"
            field(0, "søknad_uuid").asText() shouldBe søknadUUID.toString()
            field(0, "ident").asText() shouldBe ident
        }
    }
}
