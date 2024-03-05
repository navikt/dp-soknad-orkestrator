@file:Suppress("ktlint:standard:no-wildcard-imports")

package no.nav.dagpenger.soknad.orkestrator.opplysning

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
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
        val identSlot = slot<String>()
        val søknadIdSlot = slot<String>()
        val behandlingIdSlot = slot<String>()
        val beskrivendeIdSlot = slot<String>()

        every {
            opplysningService.hentOpplysning(
                capture(identSlot),
                capture(søknadIdSlot),
                capture(behandlingIdSlot),
                capture(beskrivendeIdSlot),
            )
        } just runs

        testRapid.sendTestMessage(opplysning_behov_event)

        identSlot.captured shouldBe "12345678987"
        søknadIdSlot.captured shouldBe "87bad9ca-3165-4892-ab8f-a37ee9c22298"
        behandlingIdSlot.captured shouldBe "c777cdb5-0518-4cd7-b171-148c8c6401c3"
        beskrivendeIdSlot.captured shouldBe "faktum.dagpenger-soknadsdato"
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
