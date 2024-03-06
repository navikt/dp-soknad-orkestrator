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
        testRapid.sendTestMessage(opplysning_behov_event)

        verify(exactly = 1) { opplysningService.hentOpplysning(any(), any(), any(), any()) }
    }

    @Test
    fun `vi mottar ikke opplysningsbehov dersom påkrevd felt mangler`() {
        testRapid.sendTestMessage(opplysning_behov_event_mangler_ident)

        verify(exactly = 0) { opplysningService.hentOpplysning(any(), any(), any(), any()) }
    }

    @Test
    fun `vi mottar ikke opplysningsbehov dersom den har løsning`() {
        testRapid.sendTestMessage(opplysning_behov_event_med_løsning)

        verify(exactly = 0) { opplysningService.hentOpplysning(any(), any(), any(), any()) }
    }

    @Test
    fun `vi mottar ikke opplysningsbehov dersom opplysning ikke er en del av behov-urn `() {
        testRapid.sendTestMessage(opplysning_behov_event_uten_opplysning_i_behov_urn)

        verify(exactly = 0) { opplysningService.hentOpplysning(any(), any(), any(), any()) }
    }
}

private val opplysning_behov_event =
    //language=JSON
    """
     {
    "@event_name": "behov",
    "ident": "12345678987",
    "søknad_id": "87bad9ca-3165-4892-ab8f-a37ee9c22298",
    "behandling_id": "c777cdb5-0518-4cd7-b171-148c8c6401c3",
    "@behov": [
      "urn:opplysning:dagpenger-soknadsdato"
    ]
    }
    """.trimIndent()

private val opplysning_behov_event_mangler_ident =
    //language=JSON
    """
     {
    "@event_name": "behov",
    "søknad_id": "87bad9ca-3165-4892-ab8f-a37ee9c22298",
    "behandling_id": "c777cdb5-0518-4cd7-b171-148c8c6401c3",
    "@behov": [
      "urn:opplysning:dagpenger-soknadsdato"
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
        "urn:opplysning:dagpenger-soknadsdato"
      ],
      "@løsning": {
        "urn:opplysning:dagpenger-soknadsdato:hypotese": "12.03.2024"
      }
    }
    """.trimIndent()

private val opplysning_behov_event_uten_opplysning_i_behov_urn =
    //language=JSON
    """
    {
      "@event_name": "behov",
      "ident": "12345678987",
      "søknad_id": "87bad9ca-3165-4892-ab8f-a37ee9c22298",
      "behandling_id": "c777cdb5-0518-4cd7-b171-148c8c6401c3",
      "@behov": [
        "urn:ikkeopplysning:dagpenger-soknadsdato"
      ]
    }
    """.trimIndent()
