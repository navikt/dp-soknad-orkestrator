package no.nav.dagpenger.soknad.orkestrator.søknad.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.mottak.MeldingOmEttersendingMottak.Companion.BEHOV
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonRepository
import java.time.LocalDateTime.now
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertFailsWith

class MeldingOmEttersendingMottakTest {
    private val søknadId = UUID.randomUUID()
    private val ident = "12345678901"
    private val journalpostId = "12316461"
    private val journalførtTidspunkt = now()
    private val seksjonId = "barnetillegg" as String
    private val seksjonData = "seksjonsdata"
    private val rapidsConnection = TestRapid()
    private val søknadRepository = mockk<SøknadRepository>(relaxed = true)
    private val seksjonRepository = mockk<SeksjonRepository>(relaxed = true)

    init {
        MeldingOmEttersendingMottak(rapidsConnection, søknadRepository, seksjonRepository)
    }

    @Test
    fun `onPacket leser melding og behandler som forventet`() {
        val ident = "12345678901"

        rapidsConnection.sendTestMessage(journalførSøknadPdfOgVedleggMelding())
        verify { søknadRepository.verifiserAtSøknadEksistererOgTilhørerIdent(søknadId, ident) }
        verify { seksjonRepository.lagreDokumentasjonskravEttersending(søknadId, ident, seksjonId, seksjonData) }
    }

    @Test
    fun `onPacket leser melding og behandler med ulik ident skal feile`() {
        coEvery {
            søknadRepository.verifiserAtSøknadEksistererOgTilhørerIdent(søknadId, ident)
        } throws IllegalArgumentException("Søknad $søknadId tilhører ikke identen som gjør kallet")

        assertFailsWith<IllegalArgumentException> {
            rapidsConnection.sendTestMessage(journalførSøknadPdfOgVedleggMelding())
        }

        verify { søknadRepository.verifiserAtSøknadEksistererOgTilhørerIdent(søknadId, ident) }
        verify(exactly = 0) { seksjonRepository.lagreDokumentasjonskravEttersending(søknadId, ident, seksjonId, seksjonData) }
    }

    private fun journalførSøknadPdfOgVedleggMelding() =
        """
        {
          "@id": "72847f73-23e3-489d-940d-fc0a01a62235",
          "@event_name": "behov",
          "@behov": ["journalfør_ettersending_av_dokumentasjon"],
          "@opprettet": "2025-01-26T15:30:27.899791748",
          "@final": true,
          "@løsning": {
            "$BEHOV" :  {
               "journalpostId": "$journalpostId", 
               "journalførtTidspunkt": "$journalførtTidspunkt",
               "dokumentasjonskravJson": "$seksjonData",
               "seksjonId": "$seksjonId"
            }
          },
          "ident": "$ident",
          "søknadId": "$søknadId"
        }
        """.trimIndent()
}
