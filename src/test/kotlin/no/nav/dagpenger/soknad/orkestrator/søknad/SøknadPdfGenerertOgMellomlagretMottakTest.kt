package no.nav.dagpenger.soknad.orkestrator.søknad

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.soknad.orkestrator.søknad.behov.BehovForJournalføringAvSøknadPdfOgVedlegg
import no.nav.dagpenger.soknad.orkestrator.søknad.mottak.SøknadPdfGenerertOgMellomlagretMottak
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonService
import no.nav.dagpenger.soknad.orkestrator.utils.asUUID
import org.junit.jupiter.api.BeforeEach
import java.util.UUID
import kotlin.test.Test

class SøknadPdfGenerertOgMellomlagretMottakTest {
    private val søknadId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
    private val ident = "12345678903"
    private val rapidsConnection = TestRapid()
    private val søknadService: SøknadService = mockk<SøknadService>(relaxed = true)
    private val seksjonService: SeksjonService = mockk<SeksjonService>(relaxed = true)

    init {
        SøknadPdfGenerertOgMellomlagretMottak(rapidsConnection, søknadService, seksjonService)
    }

    @BeforeEach
    fun setUp() {
        rapidsConnection.reset()
    }

    @Test
    fun `onPacket leser melding og behandler den som forventet`() {
        every { seksjonService.hentAlleSeksjonerMedSeksjonIdSomNøkkel(any(), any()) } returns søknadsData
        rapidsConnection.sendTestMessage(genererOgMellomlagreSøknadPdfLøsning)

        rapidsConnection.inspektør.size shouldBe 1
        verify { seksjonService.hentAlleSeksjonerMedSeksjonIdSomNøkkel(ident, søknadId) }

        with(rapidsConnection.inspektør) {
            message(0)["søknadId"].asUUID() shouldBe søknadId
            message(0)["ident"].asText() shouldBe ident
            message(0)["data"].asText() shouldBe
                """{"seksjoner":{"personalia":"{\"navn\":\"Ola Nordmann\",\"fødselsnummer\":\"12345678903\"}","verneplikt":"{\"avtjentVerneplikt\":\"ja\"}"},"søknad_uuid":"123e4567-e89b-12d3-a456-426614174000","fødselsnummer":"12345678903","versjon_navn":"Dagpenger_v2"}"""
            message(0)[BehovForJournalføringAvSøknadPdfOgVedlegg.BEHOV].asText() shouldNotBe null
        }
    }

    private val søknadsData =
        mapOf(
            "personalia" to """{"navn":"Ola Nordmann","fødselsnummer":"$ident"}""",
            "verneplikt" to """{"avtjentVerneplikt":"ja"}""",
        )

    private val genererOgMellomlagreSøknadPdfLøsning =
        //language=json
        """
        {
          "@id": "675eb2c2-bfba-4939-926c-cf5aac73d163",
          "@event_name": "behov",
          "@behov": ["generer_og_mellomlagre_søknad_pdf"],
          "@opprettet": "2024-02-21T11:00:27.899791748",
          "@final": true,
          "@løsning": {"generer_og_mellomlagre_søknad_pdf": [
               {
                 "metainfo": {
                   "innhold": "netto.pdf",
                   "filtype": "PDF", 
                   "variant": "NETTO"
                 },
                 "urn": "urn:vedlegg:soknadId/netto.pdf"
               },
               {
                 "metainfo": {
                   "innhold": "brutto.pdf",
                   "filtype": "PDF",
                   "variant": "BRUTTO"
                 },
                 "urn": "urn:vedlegg:soknadId/brutto.pdf"
               }
             ]},
          "ident": "$ident",
          "søknadId": "$søknadId",
          "data": "[{\"seksjonId\":\"personalia\",\"data\":\"{\\\"navn\\\":\\\"Ola Nordmann\\\",\\\"fødselsnummer\\\":\\\"$ident\\\"}\"}]"
        }
        """.trimIndent()
}
