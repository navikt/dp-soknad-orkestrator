package no.nav.dagpenger.soknad.orkestrator.søknad

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.util.UUID

class SøknadServiceTest {
    @Test
    fun `vi kan mappe LegacySøknad til Søknad`() {
        val id = UUID.randomUUID()
        val søknadUUID = UUID.randomUUID()
        val journalpostId = "637582711"

        val legacySøknad =
            LegacySøknad(
                id = id,
                opprettet = 20.februar.atStartOfDay(),
                fødselsnummer = "12345678901",
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
            fødselsnummer shouldBe "12345678901"
            opplysninger.size shouldBe 1
            opplysninger.first().beskrivendeId() shouldBe "faktum.hvilket-land-bor-du-i"
            opplysninger.first().svar() shouldBe listOf("NOR")
        }
    }
}
