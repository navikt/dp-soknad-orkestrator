package no.nav.dagpenger.soknad.orkestrator.søknad.pdf

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.soknad.orkestrator.søknad.Søknad
import no.nav.dagpenger.soknad.orkestrator.søknad.SøknadPersonalia
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadPersonaliaRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonRepository
import org.junit.jupiter.api.BeforeEach
import java.time.LocalDateTime
import java.util.UUID.randomUUID
import kotlin.test.Test

class PdfPayloadServiceTest {
    private val søknadRepository = mockk<SøknadRepository>()
    private val søknadPersonaliaRepository = mockk<SøknadPersonaliaRepository>()
    private val seksjonRepository = mockk<SeksjonRepository>()

    private val søknadId = randomUUID()
    private val ident = "987654321"

    private val pdfPayloadService =
        PdfPayloadService(
            søknadRepository,
            søknadPersonaliaRepository,
            seksjonRepository,
        )

    @BeforeEach
    fun setUp() {
        every { søknadRepository.hent(any()) } returns
            Søknad(
                ident = ident,
                innsendtTidspunkt = LocalDateTime.of(2020, 12, 31, 23, 59, 59),
            )
        every { søknadPersonaliaRepository.hent(any(), any()) } returns
            SøknadPersonalia(
                søknadId,
                ident,
                "fornavn",
                "mellomnavn",
                "etternavn",
                "32",
                "adresselinje1",
                "adresselinje2",
                "adresselinje3",
                "1234",
                "poststed",
                "LAN",
                "land",
                "97143830733",
            )
        every { seksjonRepository.hentPdfGrunnlag(any(), any()) } returns
            listOf(
                this::class.java
                    .getResource("/testdata/pdf-service-test-pdf-grunnlag-egen-næring.json")!!
                    .readText(Charsets.UTF_8),
                this::class.java
                    .getResource("/testdata/pdf-service-test-pdf-grunnlag-en-annen-seksjon.json")!!
                    .readText(Charsets.UTF_8),
                this::class.java
                    .getResource("/testdata/pdf-service-test-pdf-grunnlag-annen-pengestøtte.json")!!
                    .readText(Charsets.UTF_8),
            )
        every { seksjonRepository.hentDokumentasjonskrav(any(), any()) } returns
            listOf(
                this::class.java
                    .getResource("/testdata/pdf-service-test-dokumentasjonskrav.json")!!
                    .readText(Charsets.UTF_8),
            )
    }

    @Test
    fun `genererNettoPdfPayload returnerer forventet resultat hvis søknad og personalia for søknaden eksisterer`() {
        val nettoPdfPayload =
            pdfPayloadService.genererNettoPdfPayload(
                randomUUID(),
                ident,
            )

        nettoPdfPayload shouldBe
            this::class.java
                .getResource("/testdata/genererNettoPdfPayload-forventet-resultat.txt")!!
                .readText(Charsets.UTF_8)
    }

    @Test
    fun `genererNettoPdfPayload kaster exception hvis søknaden ikke eksisterer`() {
        val søknadId = randomUUID()
        every { søknadRepository.hent(any()) } returns null

        val exception =
            shouldThrow<IllegalStateException> {
                pdfPayloadService.genererNettoPdfPayload(
                    søknadId,
                    ident,
                )
            }

        exception.message shouldBe "Fant ikke søknad $søknadId"
    }

    @Test
    fun `genererNettoPdfPayload kaster exception hvis personalia for søknaden ikke eksisterer`() {
        val søknadId = randomUUID()
        every { søknadPersonaliaRepository.hent(any(), any()) } returns null

        val exception =
            shouldThrow<IllegalStateException> {
                pdfPayloadService.genererNettoPdfPayload(
                    søknadId,
                    ident,
                )
            }

        exception.message shouldBe "Fant ikke personalia for søknad $søknadId"
    }

    @Test
    fun `genererNettoPdfPayload kaster exception pdf-grunnlag for søknaden ikke eksisterer`() {
        val søknadId = randomUUID()
        every { seksjonRepository.hentPdfGrunnlag(any(), any()) } returns emptyList()

        val exception =
            shouldThrow<IllegalStateException> {
                pdfPayloadService.genererNettoPdfPayload(
                    søknadId,
                    ident,
                )
            }

        exception.message shouldBe "Fant ikke PDF-grunnlag for søknad $søknadId"
    }

    @Test
    fun `genererBruttoPdfPayload returnerer forventet resultat hvis søknad og personalia for søknaden eksisterer`() {
        val bruttoPdfPayload =
            pdfPayloadService.genererBruttoPdfPayload(
                randomUUID(),
                ident,
            )

        bruttoPdfPayload shouldBe
            this::class.java
                .getResource("/testdata/genererBruttoPdfPayload-forventet-resultat.txt")!!
                .readText(Charsets.UTF_8)
    }

    @Test
    fun `genererBruttoPdfPayload kaster exception hvis søknaden ikke eksisterer`() {
        val søknadId = randomUUID()
        every { søknadRepository.hent(any()) } returns null

        val exception =
            shouldThrow<IllegalStateException> {
                pdfPayloadService.genererBruttoPdfPayload(
                    søknadId,
                    ident,
                )
            }

        exception.message shouldBe "Fant ikke søknad $søknadId"
    }

    @Test
    fun `genererBruttoPdfPayload kaster exception hvis personalia for søknaden ikke eksisterer`() {
        val søknadId = randomUUID()
        every { søknadPersonaliaRepository.hent(any(), any()) } returns null

        val exception =
            shouldThrow<IllegalStateException> {
                pdfPayloadService.genererBruttoPdfPayload(
                    søknadId,
                    ident,
                )
            }

        exception.message shouldBe "Fant ikke personalia for søknad $søknadId"
    }

    @Test
    fun `genererBruttoPdfPayload kaster exception pdf-grunnlag for søknaden ikke eksisterer`() {
        val søknadId = randomUUID()
        every { seksjonRepository.hentPdfGrunnlag(any(), any()) } returns emptyList()

        val exception =
            shouldThrow<IllegalStateException> {
                pdfPayloadService.genererBruttoPdfPayload(
                    søknadId,
                    ident,
                )
            }

        exception.message shouldBe "Fant ikke PDF-grunnlag for søknad $søknadId"
    }
}
