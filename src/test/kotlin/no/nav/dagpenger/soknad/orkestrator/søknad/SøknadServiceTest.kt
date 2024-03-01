package no.nav.dagpenger.soknad.orkestrator.søknad

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.util.UUID

class SøknadServiceTest {
    @Test
    fun `vi kan mappe LegacySøknad til Søknad`() {
        val id = UUID.randomUUID()
        val journalpostId = UUID.randomUUID()

        val legacySøknad =
            LegacySøknad(
                id = "$id",
                opprettet = "2024-02-21T11:00:27.899791748",
                fødselsnummer = "12345678901",
                journalpostId = "$journalpostId",
                søknadsData =
                    SøknadsData(
                        opprettet = "2024-02-21T11:00:27.899791748",
                        søknad_uuid = "123e4567-e89b-12d3-a456-426614174000",
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
