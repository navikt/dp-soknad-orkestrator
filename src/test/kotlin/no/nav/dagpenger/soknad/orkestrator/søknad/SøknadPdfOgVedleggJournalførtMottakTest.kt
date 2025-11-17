package no.nav.dagpenger.soknad.orkestrator.søknad

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.mottak.SøknadPdfOgVedleggJournalførtMottak
import no.nav.dagpenger.soknad.orkestrator.søknad.mottak.SøknadPdfOgVedleggJournalførtMottak.Companion.BEHOV
import java.time.LocalDateTime.now
import java.util.UUID.randomUUID
import kotlin.test.Test

class SøknadPdfOgVedleggJournalførtMottakTest {
    private val søknadId = randomUUID()
    private val ident = "30259704304"
    private val journalpostId = "12316461"
    private val journalførtTidspunkt = now()
    private val rapidsConnection = TestRapid()
    private val søknadRepository = mockk<SøknadRepository>(relaxed = true)

    init {
        SøknadPdfOgVedleggJournalførtMottak(rapidsConnection, søknadRepository)
    }

    @Test
    fun `onPacket leser melding og behandler den som forventet`() {
        coEvery { søknadRepository.hent(any()) } returns Søknad(søknadId, ident)

        rapidsConnection.sendTestMessage(journalførSøknadPdfOgVedleggMelding)

        verify { søknadRepository.markerSøknadSomJournalført(søknadId, journalpostId, journalførtTidspunkt) }
    }

    @Test
    fun `onPacket behandler ikke melding hvis søknadId ikke eksisterer`() {
        coEvery { søknadRepository.hent(any()) } returns null

        rapidsConnection.sendTestMessage(journalførSøknadPdfOgVedleggMelding)

        verify(exactly = 0) {
            søknadRepository.markerSøknadSomJournalført(
                søknadId,
                journalpostId,
                journalførtTidspunkt,
            )
        }
        rapidsConnection.inspektør.size shouldBe 0
    }

    @Test
    fun `onPacket behandler ikke melding hvis innsendt søknadId har en annen ident enn lagret søknadId`() {
        coEvery { søknadRepository.hent(any()) } returns Søknad(søknadId, "en_annen_ident")

        rapidsConnection.sendTestMessage(journalførSøknadPdfOgVedleggMelding)

        verify(exactly = 0) {
            søknadRepository.markerSøknadSomJournalført(
                søknadId,
                journalpostId,
                journalførtTidspunkt,
            )
        }
        rapidsConnection.inspektør.size shouldBe 0
    }

    private val journalførSøknadPdfOgVedleggMelding =
        //language=json
        """
        {
          "@id": "675eb2c2-bfba-4939-926c-cf5aac73d163",
          "@event_name": "behov",
          "@behov": ["journalfør_søknad_pdf_og_vedlegg"],
          "@opprettet": "2024-02-21T11:00:27.899791748",
          "@final": true,
          "@løsning": {"$BEHOV" :  {"journalpostId": "$journalpostId", "journalførtTidspunkt": "$journalførtTidspunkt"}},
          "ident": "$ident",
          "søknadId": "$søknadId"
        }
        """.trimIndent()
}
