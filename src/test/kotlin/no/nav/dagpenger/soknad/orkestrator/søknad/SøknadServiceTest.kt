package no.nav.dagpenger.soknad.orkestrator.søknad

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
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
                opprettet = LocalDateTime.of(2024, 2, 21, 11, 0, 0),
                fødselsnummer = "12345678901",
                journalpostId = journalpostId,
                søknadsData =
                    SøknadsData(
                        opprettet = LocalDateTime.of(2024, 2, 21, 11, 0, 0),
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
