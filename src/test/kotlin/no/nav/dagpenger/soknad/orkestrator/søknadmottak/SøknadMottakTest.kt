package no.nav.dagpenger.soknad.orkestrator.søknadmottak

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.slot
import no.nav.dagpenger.soknad.orkestrator.opplysning.Søknad
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SøknadMottakTest {
    private val testRapid = TestRapid()

    init {
        SøknadMottak(rapidsConnection = testRapid)
    }

    @BeforeEach
    fun setup() {
        testRapid.reset()
        mockkConstructor(SøknadMapper::class)
    }

    @Test
    fun `Skal kunne ta i mot innsending_ferdigstilt event`() {
        val slot = slot<LegacySøknad>()
        every { anyConstructed<SøknadMapper>().toSøknad(capture(slot)) } returns mockk<Søknad>()

        testRapid.sendTestMessage(innsending_ferdigstilt_event)

        with(slot.captured) {
            fødselsnummer shouldBe "12345678903"
            journalpostId shouldBe "637582711"
            søknadsData.søknad_uuid shouldBe "123e4567-e89b-12d3-a456-426614174000"
        }
    }
}

private val innsending_ferdigstilt_event =
    //language=json
    """
     {
      "@id": "675eb2c2-bfba-4939-926c-cf5aac73d163",
      "@event_name": "innsending_ferdigstilt",
      "@opprettet": "2024-02-21T11:00:27.899791748",
      "journalpostId": "637582711",
      "type": "NySøknad",
      "fødselsnummer": "12345678903",
      "søknadsData": {
        "søknad_uuid": "123e4567-e89b-12d3-a456-426614174000",
        "@opprettet": "2024-02-21T11:00:27.899791748",
        "seksjoner": [
        {
          "fakta": [
          {
            "id": "6001",
            "svar": "NOR",
            "type": "land",
            "beskrivendeId": "faktum.hvilket-land-bor-du-i"
          } 
          ],
        "beskrivendeId": "bostedsland"
        } 
        ]
      }
    }
    """.trimIndent()
