package no.nav.dagpenger.soknad.orkestrator.søknad

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.dagpenger.soknad.orkestrator.utils.asUUID
import org.junit.jupiter.api.BeforeEach
import java.util.UUID
import kotlin.test.Test

class SøknadPdfGenerertOgMellomlagretMottakTest {
    private val søknadId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
    private val ident = "12345678903"
    private val rapidsConnection = TestRapid()

    init {
        SøknadPdfGenerertOgMellomlagretMottak(rapidsConnection)
    }

    @BeforeEach
    fun setUp() {
        rapidsConnection.reset()
    }

    @Test
    fun `onPacket leser melding og behandler den som forventet`() {
        rapidsConnection.sendTestMessage(genererOgMellomlagreSøknadPdfLøsning)

        rapidsConnection.inspektør.size shouldBe 1
        with(rapidsConnection.inspektør) {
            message(0)["søknadId"].asUUID() shouldBe søknadId
            message(0)["ident"].asText() shouldBe ident
            message(0)[BehovForJournalføringAvSøknadPdfOgVedlegg.BEHOV].asText() shouldNotBe null
        }
    }

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
          "søknadId": "$søknadId"
        }
        """.trimIndent()
}
