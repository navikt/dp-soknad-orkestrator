@file:Suppress("ktlint:standard:no-wildcard-imports")

package no.nav.dagpenger.soknad.orkestrator.opplysning

import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OpplysningBehovMottakTest {
    private val testRapid = TestRapid()
    private val opplysningService = mockk<OpplysningService>(relaxed = true)

    init {
        OpplysningBehovMottak(rapidsConnection = testRapid, opplysningService = opplysningService)
    }

    @BeforeEach
    fun reset() {
        testRapid.reset()
    }

    @Test
    fun `vi kan motta opplysningsbehov`() {
        val behov = listOf("Søknadstidspunkt", "JobbetUtenforNorge", "ØnskerDagpengerFraDato")
        testRapid.sendTestMessage(opplysning_behov_event(behov))

        verify(exactly = 1) { opplysningService.løsBehov(behov) }
    }

    @Test
    fun `vi mottar ikke opplysningsbehov dersom påkrevd felt mangler`() {
        testRapid.sendTestMessage(opplysning_behov_event_mangler_ident)

        verify(exactly = 0) { opplysningService.løsBehov(any()) }
    }

    @Test
    fun `vi mottar ikke opplysningsbehov dersom den har løsning`() {
        testRapid.sendTestMessage(opplysning_behov_event_med_løsning)

        verify(exactly = 0) { opplysningService.hentOpplysning(any(), any(), any(), any()) }
    }
}

private fun opplysning_behov_event(
    behov: List<String> =
        listOf(
            "Søknadstidspunkt",
            "JobbetUtenforNorge",
            "ØnskerDagpengerFraDato",
            "EøsArbeid",
            "KanJobbeDeltid",
            "HelseTilAlleTyperJobb",
            "KanJobbeHvorSomHelst",
            "VilligTilÅBytteYrke",
        ),
): String {
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
        "Søknadstidspunkt"
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
        "Søknadstidspunkt"
      ],
      "@løsning": {
        "Søknadstidspunkt": "12.03.2024"
      }
    }
    """.trimIndent()
