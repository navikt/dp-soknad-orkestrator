package no.nav.dagpenger.soknad.orkestrator.søknad

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import java.util.UUID
import kotlin.test.Test

class SøknadSlettetMottakTest {
    private val testRapid = TestRapid()
    private val søknadService = mockk<SøknadService>(relaxed = true)

    private val ident = "1234567890"
    private val søknadId = UUID.randomUUID()

    @BeforeEach
    fun setup() {
        SøknadSlettetMottak(testRapid, søknadService)
    }

    @Test
    fun `Skal slette søknad når søknad_slettet event mottas`() {
        testRapid.sendTestMessage(søknadSlettetEvent)

        verify { søknadService.slett(søknadId, ident) }
    }

    @Test
    fun `Tar ikke i mot søknad_slettet event når påkrevd felt ident mangler`() {
        testRapid.sendTestMessage(søknadSlettetEventUtenIdent)

        verify(exactly = 0) { søknadService.slett(any(), any()) }
    }

    @Test
    fun `Tar ikke i mot søknad_slettet event når påkrevd felt søknadId mangler`() {
        testRapid.sendTestMessage(søknadSlettetEventUtenSøknadId)

        verify(exactly = 0) { søknadService.slett(any(), any()) }
    }

    private val søknadSlettetEvent =
        //language=json
        """
        {
          "@event_name": "søknad_slettet",
          "søknad_uuid": "$søknadId",
          "ident": "$ident"
        }
        """.trimIndent()

    private val søknadSlettetEventUtenIdent =
        //language=json
        """
        {
          "@event_name": "søknad_slettet",
          "søknad_uuid": "$søknadId"
        }
        """.trimIndent()

    private val søknadSlettetEventUtenSøknadId =
        //language=json
        """
        {
          "@event_name": "søknad_slettet",
          "ident": "$ident"
        }
        """.trimIndent()
}
