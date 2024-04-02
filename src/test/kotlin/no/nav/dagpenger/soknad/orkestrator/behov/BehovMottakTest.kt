@file:Suppress("ktlint:standard:no-wildcard-imports")

package no.nav.dagpenger.soknad.orkestrator.behov

import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BehovMottakTest {
    private val testRapid = TestRapid()
    private val behovLøserFactory = mockk<BehovløserFactory>(relaxed = true)

    init {
        BehovMottak(rapidsConnection = testRapid, behovLøserFactory = behovLøserFactory)
    }

    @BeforeEach
    fun reset() {
        testRapid.reset()
    }

    @Test
    fun `vi kan motta opplysningsbehov ØnskerDagpengerFraDato`() {
        val behov = "ØnskerDagpengerFraDato"
        testRapid.sendTestMessage(opplysning_behov_event(listOf(behov)))

        verify(exactly = 1) { behovLøserFactory.behovløserFor(behov) }
    }

    @Test
    fun `vi kan motta opplysningsbehov EøsArbeid`() {
        val behov = "EøsArbeid"
        testRapid.sendTestMessage(opplysning_behov_event(listOf(behov)))

        verify(exactly = 1) { behovLøserFactory.behovløserFor(behov) }
    }

    @Test
    fun `vi kan motta opplysningsbehov KanJobbeDeltid`() {
        val behov = "KanJobbeDeltid"
        testRapid.sendTestMessage(opplysning_behov_event(listOf(behov)))

        verify(exactly = 1) { behovLøserFactory.behovløserFor(behov) }
    }

    @Test
    fun `vi kan motta opplysningsbehov HelseTilAlleTyperJobb`() {
        val behov = "HelseTilAlleTyperJobb"
        testRapid.sendTestMessage(opplysning_behov_event(listOf(behov)))

        verify(exactly = 1) { behovLøserFactory.behovløserFor(behov) }
    }

    @Test
    fun `vi kan motta opplysningsbehov KanJobbeHvorSomHelst`() {
        val behov = "KanJobbeHvorSomHelst"
        testRapid.sendTestMessage(opplysning_behov_event(listOf(behov)))

        verify(exactly = 1) { behovLøserFactory.behovløserFor(behov) }
    }

    @Test
    fun `vi kan motta opplysningsbehov VilligTilÅBytteYrke`() {
        val behov = "VilligTilÅBytteYrke"
        testRapid.sendTestMessage(opplysning_behov_event(listOf(behov)))

        verify(exactly = 1) { behovLøserFactory.behovløserFor(behov) }
    }

    @Test
    fun `vi kan motta opplysningsbehov Søknadstidspunkt`() {
        val behov = "Søknadstidspunkt"
        testRapid.sendTestMessage(opplysning_behov_event(listOf(behov)))

        verify(exactly = 1) { behovLøserFactory.behovløserFor(behov) }
    }

    @Test
    fun `vi kan motta opplysningsbehov JobbetUtenforNorge`() {
        val behov = "JobbetUtenforNorge"
        testRapid.sendTestMessage(opplysning_behov_event(listOf(behov)))

        verify(exactly = 1) { behovLøserFactory.behovløserFor(behov) }
    }

    @Test
    fun `vi mottar ikke opplysningsbehov dersom påkrevd felt mangler`() {
        testRapid.sendTestMessage(opplysning_behov_event_mangler_ident)

        verify(exactly = 0) { behovLøserFactory.behovløserFor(any()) }
    }

    @Test
    fun `vi mottar ikke opplysningsbehov dersom den har løsning`() {
        testRapid.sendTestMessage(opplysning_behov_event_med_løsning)

        verify(exactly = 0) { behovLøserFactory.behovløserFor(any()) }
    }
}

private fun opplysning_behov_event(behov: List<String>): String {
    val behovString = behov.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }
    //language=JSON
    return """
        {
          "@event_name": "behov",
          "ident": "12345678987",
          "søknad_id": "87bad9ca-3165-4892-ab8f-a37ee9c22298",
          "behandling_id": "c777cdb5-0518-4cd7-b171-148c8c6401c3",
          "@behov": $behovString
        }
        """.trimIndent()
}

private val opplysning_behov_event_mangler_ident =
    //language=JSON
    """
    {
      "@event_name": "behov",
      "søknad_id": "87bad9ca-3165-4892-ab8f-a37ee9c22298",
      "behandling_id": "c777cdb5-0518-4cd7-b171-148c8c6401c3",
      "@behov": [
        "ØnskerDagpengerFraDato"
      ]
    }
    """.trimIndent()

private val opplysning_behov_event_med_løsning =
    //language=JSON
    """
    {
      "@event_name": "behov",
      "ident": "12345678987",
      "søknad_id": "87bad9ca-3165-4892-ab8f-a37ee9c22298",
      "behandling_id": "c777cdb5-0518-4cd7-b171-148c8c6401c3",
      "@behov": [
        "ØnskerDagpengerFraDato"
      ],
      "@løsning": {
        "ØnskerDagpengerFraDato": "12.03.2024"
      }
    }
    """.trimIndent()
