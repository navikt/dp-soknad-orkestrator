package no.nav.dagpenger.soknad.orkestrator.søknad

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.soknad.orkestrator.søknad.Tilstand.INNSENDT
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.melding.MeldingOmSøknadKlarTilJournalføringMottak
import no.nav.dagpenger.soknad.orkestrator.søknad.pdf.PdfPayloadService
import org.junit.jupiter.api.BeforeEach
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test

class MeldingOmSøknadKlarTilJournalføringMottakTest {
    private val søknadId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
    private val innsendtTidspunkt = LocalDateTime.parse("2024-02-21T11:00:27.899791748")
    private val ident = "12345678903"
    private val rapidsConnection = TestRapid()
    private val søknadRepository = mockk<SøknadRepository>(relaxed = true)
    private val pdfPayloadService = mockk<PdfPayloadService>(relaxed = true)

    init {
        MeldingOmSøknadKlarTilJournalføringMottak(rapidsConnection, søknadRepository, pdfPayloadService)
    }

    @BeforeEach
    fun setUp() {
        rapidsConnection.reset()
    }

    @Test
    fun `onPacket leser melding og behandler den som forventet`() {
        coEvery { søknadRepository.hent(any()) } returns Søknad(søknadId, ident)

        rapidsConnection.sendTestMessage(søknadKlarTilJournalføringEvent)

        verify { søknadRepository.markerSøknadSomInnsendt(søknadId, ident, innsendtTidspunkt) }
        verify { pdfPayloadService.genererBruttoPdfPayload(søknadId, ident) }
        verify { pdfPayloadService.genererNettoPdfPayload(søknadId, ident) }
        rapidsConnection.inspektør.size shouldBe 2
        rapidsConnection.inspektør.message(0)["@behov"][0].asText() shouldBe "generer_og_mellomlagre_søknad_pdf"
        rapidsConnection.inspektør.message(1)["@event_name"].asText() shouldBe "søknad_endret_tilstand"
    }

    @Test
    fun `onPacket behandler ikke melding hvis søknad har tilstand ulik PÅBEGYNT`() {
        coEvery { søknadRepository.hent(any()) } returns Søknad(søknadId, ident, INNSENDT)

        rapidsConnection.sendTestMessage(søknadKlarTilJournalføringEvent)

        verify(exactly = 0) { søknadRepository.markerSøknadSomInnsendt(søknadId, ident, innsendtTidspunkt) }
        rapidsConnection.inspektør.size shouldBe 0
    }

    @Test
    fun `onPacket behandler ikke melding hvis søknadId ikke eksisterer`() {
        coEvery { søknadRepository.hent(any()) } returns null

        rapidsConnection.sendTestMessage(søknadKlarTilJournalføringEvent)

        verify(exactly = 0) { søknadRepository.markerSøknadSomInnsendt(søknadId, ident, innsendtTidspunkt) }
        rapidsConnection.inspektør.size shouldBe 0
    }

    @Test
    fun `onPacket behandler ikke melding hvis innsendt søknadId har en annen ident enn lagret søknadId`() {
        coEvery { søknadRepository.hent(any()) } returns Søknad(søknadId, "en_annen_ident")

        rapidsConnection.sendTestMessage(søknadKlarTilJournalføringEvent)

        verify(exactly = 0) { søknadRepository.markerSøknadSomInnsendt(søknadId, ident, innsendtTidspunkt) }
        rapidsConnection.inspektør.size shouldBe 0
    }

    private val søknadKlarTilJournalføringEvent =
        //language=json
        """
        {
          "@id": "675eb2c2-bfba-4939-926c-cf5aac73d163",
          "@event_name": "${MeldingOmSøknadKlarTilJournalføringMottak.EVENT_NAME}",
          "@opprettet": "2024-02-21T11:00:27.899791748",
          "ident": "$ident",
          "søknadId": "$søknadId",
          "innsendtTidspunkt": "$innsendtTidspunkt"
        }
        """.trimIndent()
}
