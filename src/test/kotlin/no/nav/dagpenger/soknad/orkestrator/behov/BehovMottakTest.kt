@file:Suppress("ktlint:standard:no-wildcard-imports")

package no.nav.dagpenger.soknad.orkestrator.behov

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.ØnskerDagpengerFraDato
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.UUID

class BehovMottakTest {
    private val testRapid = TestRapid()
    private val behovløserFactory =
        mockk<BehovløserFactory>(relaxed = true).also {
            every { it.behov() } returns BehovløserFactory.Behov.entries.map { it.name }
        }

    init {
        BehovMottak(rapidsConnection = testRapid, behovløserFactory = behovløserFactory)
    }

    companion object {
        @JvmStatic
        fun behovProvider() = BehovløserFactory.Behov.entries.map { arrayOf(it) }
    }

    @BeforeEach
    fun reset() {
        testRapid.reset()
    }

    @ParameterizedTest
    @MethodSource("behovProvider")
    fun `vi kan motta opplysningsbehov`(behov: BehovløserFactory.Behov) {
        testRapid.sendTestMessage(opplysning_behov_event(listOf(behov.name)))

        verify(exactly = 1) { behovløserFactory.behovløserFor(behov) }
    }

    @Test
    fun `vi mottar ikke opplysningsbehov dersom påkrevd felt mangler`() {
        testRapid.sendTestMessage(opplysning_behov_event_mangler_ident)

        verify(exactly = 0) { behovløserFactory.behovløserFor(any()) }
    }

    @Test
    fun `vi mottar ikke opplysningsbehov dersom den har løsning`() {
        testRapid.sendTestMessage(opplysning_behov_event_med_løsning)

        verify(exactly = 0) { behovløserFactory.behovløserFor(any()) }
    }

    @Test
    fun `Vi filtrerer ut mottatte behov som vi ikke skal løse`() {
        val behov = listOf(ØnskerDagpengerFraDato.name, "UkjentBehov")
        lagJsonMessage(behov).mottatteBehov() shouldBe listOf(ØnskerDagpengerFraDato.name)
    }
}

fun lagJsonMessage(behov: List<String>): JsonMessage =
    JsonMessage.newMessage(
        eventName = "behov",
        map =
            mapOf(
                "ident" to "12345678987",
                "søknadId" to UUID.randomUUID(),
                "@behov" to behov,
            ),
    ).apply { this.requireKey("ident", "søknadId", "@behov") }

private fun opplysning_behov_event(behov: List<String>): String {
    val behovString = behov.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }
    //language=JSON
    return """
        {
          "@event_name": "behov",
          "bruk-søknad-orkestrator": true,
          "ident": "12345678987",
          "søknadId": "87bad9ca-3165-4892-ab8f-a37ee9c22298",
          "behandlingId": "c777cdb5-0518-4cd7-b171-148c8c6401c3",
          "@behovId": "c777cdb5-0518-4cd7-b171-148c8c6401c4",
          "@behov": $behovString
        }
        """.trimIndent()
}

private val opplysning_behov_event_mangler_ident =
    //language=JSON
    """
    {
      "@event_name": "behov",
      "bruk-søknad-orkestrator": true,
      "søknadId": "87bad9ca-3165-4892-ab8f-a37ee9c22298",
      "behandlingId": "c777cdb5-0518-4cd7-b171-148c8c6401c3",
      "@behovId": "c777cdb5-0518-4cd7-b171-148c8c6401c4",
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
      "bruk-søknad-orkestrator": true,
      "ident": "12345678987",
      "søknadId": "87bad9ca-3165-4892-ab8f-a37ee9c22298",
      "behandlingId": "c777cdb5-0518-4cd7-b171-148c8c6401c3",
      "@behovId": "c777cdb5-0518-4cd7-b171-148c8c6401c4",
      "@behov": [
        "ØnskerDagpengerFraDato"
      ],
      "@løsning": {
        "ØnskerDagpengerFraDato": "12.03.2024"
      }
    }
    """.trimIndent()
