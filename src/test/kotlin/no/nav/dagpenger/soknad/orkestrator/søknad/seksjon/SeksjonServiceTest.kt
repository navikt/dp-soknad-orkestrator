package no.nav.dagpenger.soknad.orkestrator.søknad.seksjon

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.soknad.orkestrator.søknad.SøknadService
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test

class SeksjonServiceTest {
    private val seksjonRepository = mockk<SeksjonRepository>(relaxed = true)
    private val søknadRepository = mockk<SøknadRepository>(relaxed = true)
    private val søknadService = mockk<SøknadService>(relaxed = true)
    private val rapidsConnection = mockk<RapidsConnection>(relaxed = true)
    private val seksjonService = SeksjonService(seksjonRepository, søknadRepository, søknadService)

    init {
        seksjonService.setRapidsConnection(rapidsConnection)
    }

    private val dokumentasjonskravMedToDokumenter =
        """
        [
          {
            "skjemakode": "T8",
            "bundle": {
              "filnavn": "vedlegg1.pdf",
              "urn": "urn:vedlegg:abc/T8"
            }
          },
          {
            "skjemakode": "O2",
            "bundle": {
              "filnavn": "vedlegg2.pdf",
              "urn": "urn:vedlegg:abc/O2"
            }
          }
        ]
        """.trimIndent()

    private val dokumentasjonskravMedTomBundle =
        """
        [
          {
            "skjemakode": "T8",
            "bundle": {}
          }
        ]
        """.trimIndent()

    @Test
    fun `lagre kaller seksjonRepository med riktige parametere og publiserer melding`() {
        val søknadId = UUID.randomUUID()
        val ident = "12345678900"
        val seksjonId = "0"
        val seksjonsvar = """{"svar": "ja"}"""
        val dokumentasjonskrav = """[{"skjemakode": "O2"}]"""
        val pdfGrunnlag = """{"pdf": "data"}"""

        val nå = LocalDateTime.now()
        every {
            seksjonRepository.hentSeksjonMetadata(søknadId, ident, seksjonId)
        } returns
            SeksjonMedTidstempler(
                seksjonId = seksjonId,
                data = seksjonsvar,
                opprettet = nå,
                oppdatert = null,
            )

        seksjonService.lagre(
            søknadId = søknadId,
            ident = ident,
            seksjonId = seksjonId,
            seksjonsvar = seksjonsvar,
            dokumentasjonskrav = dokumentasjonskrav,
            pdfGrunnlag = pdfGrunnlag,
        )

        verify {
            seksjonRepository.lagre(søknadId, ident, seksjonId, seksjonsvar, dokumentasjonskrav, pdfGrunnlag)
            rapidsConnection.publish(ident, any())
        }
    }

    @Test
    fun `lagre bruker oppdatert dato når den finnes`() {
        val søknadId = UUID.randomUUID()
        val ident = "12345678900"
        val seksjonId = "0"
        val seksjonsvar = """{"svar": "ja"}"""
        val pdfGrunnlag = """{"pdf": "data"}"""

        val opprettet = LocalDateTime.now().minusHours(1)
        val oppdatert = LocalDateTime.now()
        every {
            seksjonRepository.hentSeksjonMetadata(søknadId, ident, seksjonId)
        } returns
            SeksjonMedTidstempler(
                seksjonId = seksjonId,
                data = "",
                opprettet = opprettet,
                oppdatert = oppdatert,
            )

        seksjonService.lagre(
            søknadId = søknadId,
            ident = ident,
            seksjonId = seksjonId,
            seksjonsvar = seksjonsvar,
            pdfGrunnlag = pdfGrunnlag,
        )

        verify {
            rapidsConnection.publish(ident, any())
        }
    }

    @Test
    fun `lagre uten dokumentasjonskrav`() {
        val søknadId = UUID.randomUUID()
        val ident = "12345678900"
        val seksjonId = "0"
        val seksjonsvar = """{"svar": "nei"}"""
        val pdfGrunnlag = """{"pdf": "data"}"""

        val nå = LocalDateTime.now()
        every {
            seksjonRepository.hentSeksjonMetadata(søknadId, ident, seksjonId)
        } returns
            SeksjonMedTidstempler(
                seksjonId = seksjonId,
                data = "",
                opprettet = nå,
                oppdatert = null,
            )

        seksjonService.lagre(
            søknadId = søknadId,
            ident = ident,
            seksjonId = seksjonId,
            seksjonsvar = seksjonsvar,
            dokumentasjonskrav = null,
            pdfGrunnlag = pdfGrunnlag,
        )

        verify {
            seksjonRepository.lagre(søknadId, ident, seksjonId, seksjonsvar, null, pdfGrunnlag)
            rapidsConnection.publish(ident, any())
        }
    }

    @Test
    fun `opprettDokumenterFraDokumentasjonskravEttersending setter hoveddokumentSkjemakode på første dokument`() {
        val dokumenter =
            seksjonService.opprettDokumenterFraDokumentasjonskravEttersending(
                dokumentasjonskrav = dokumentasjonskravMedToDokumenter,
                hoveddokumentSkjemakode = "04-01.03",
            )

        dokumenter.size shouldBe 2
        dokumenter[0].skjemakode shouldBe "04-01.03"
        dokumenter[1].skjemakode shouldBe "O2"
    }

    @Test
    fun `opprettDokumenterFraDokumentasjonskravEttersending beholder originale skjemakoder når hoveddokumentSkjemakode er null`() {
        val dokumenter =
            seksjonService.opprettDokumenterFraDokumentasjonskravEttersending(
                dokumentasjonskrav = dokumentasjonskravMedToDokumenter,
                hoveddokumentSkjemakode = null,
            )

        dokumenter.size shouldBe 2
        dokumenter[0].skjemakode shouldBe "T8"
        dokumenter[1].skjemakode shouldBe "O2"
    }

    @Test
    fun `opprettDokumenterFraDokumentasjonskravEttersending filtrerer ut dokumenter med tom bundle`() {
        val dokumenter =
            seksjonService.opprettDokumenterFraDokumentasjonskravEttersending(
                dokumentasjonskrav = dokumentasjonskravMedTomBundle,
                hoveddokumentSkjemakode = "04-01.03",
            )

        dokumenter.size shouldBe 0
    }

    @Test
    fun `opprettDokumenterFraDokumentasjonskravEttersending bruker skjemakode fra søknadService`() {
        every { søknadService.finnSkjemaKode(any(), any(), any()) } returns "04-16.04"

        val dokumenter =
            seksjonService.opprettDokumenterFraDokumentasjonskravEttersending(
                dokumentasjonskrav = dokumentasjonskravMedToDokumenter,
                hoveddokumentSkjemakode = "04-16.04",
            )

        dokumenter[0].skjemakode shouldBe "04-16.04"
    }
}
