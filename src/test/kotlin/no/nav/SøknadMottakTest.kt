package no.nav

import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SøknadMottakTest {
    private val testRapid =
        TestRapid().also {
            SøknadMottak(it)
        }

    @BeforeEach
    fun setup() {
        testRapid.reset()
    }

    @Test
    fun `Skal kunne ta i mot innsending_ferdigstilt event`() {
        // TODO: Teste at onPacket gjør det den skal (når vi har fått logikk der)
    }
}

//language=JSON
private val innsending_ferdigstilt_event =
    """
    {
      "type": "NySøknad",
      "fødselsnummer": "123",
      "søknadsData": {
        "søknad_uuid": "123e4567-e89b-12d3-a456-426614174000"
      },
      "@event_name": "innsending_ferdigstilt"
    }
    """.trimIndent()
