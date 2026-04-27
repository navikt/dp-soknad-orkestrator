package no.nav.dagpenger.soknad.orkestrator.søknad.seksjon

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.soknad.orkestrator.søknad.SøknadService
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import kotlin.test.Test

class SeksjonServiceTest {
    private val seksjonRepository = mockk<SeksjonRepository>(relaxed = true)
    private val søknadRepository = mockk<SøknadRepository>(relaxed = true)
    private val søknadService = mockk<SøknadService>(relaxed = true)
    private val seksjonService = SeksjonService(seksjonRepository, søknadRepository, søknadService)

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
