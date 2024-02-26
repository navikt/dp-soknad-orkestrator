package no.nav.dagpenger.soknad.orkestrator

import io.kotest.matchers.shouldBe
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import no.nav.søknad.SøknadMottak
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
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

    @Disabled
    @Test
    fun `Skal kunne ta i mot innsending_ferdigstilt event`() {
        testRapid.sendTestMessage(innsending_ferdigstilt_event)
        testRapid.inspektør.size shouldBe 1
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
