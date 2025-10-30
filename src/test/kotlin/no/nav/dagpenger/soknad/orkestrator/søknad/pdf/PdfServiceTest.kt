package no.nav.dagpenger.soknad.orkestrator.søknad.pdf

import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.soknad.orkestrator.søknad.Søknad
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonRepository
import org.junit.jupiter.api.BeforeEach
import java.time.LocalDateTime.now
import java.util.UUID.randomUUID
import kotlin.test.Test

class PdfServiceTest {
    private val søknadRepository = mockk<SøknadRepository>()
    private val seksjonRepository = mockk<SeksjonRepository>()

    @BeforeEach
    fun setUp() {
        every { søknadRepository.hent(any()) } returns Søknad(ident = "987654321", innsendtTidspunkt = now())
        every { seksjonRepository.hentPdfGrunnlag(any(), any()) } returns
            listOf(
                this::class.java.getResource("/testdata/pdf-service-test-pdf-grunnlag-egen-næring.json")!!.readText(Charsets.UTF_8),
                this::class.java.getResource("/testdata/pdf-service-test-pdf-grunnlag-en-annen-seksjon.json")!!.readText(Charsets.UTF_8),
            )
    }

    @Test
    fun testNettoPdf() {
        val nettoPdfPayload =
            PdfService(søknadRepository, seksjonRepository).genererNettoPdfPayload(
                "987654321",
                randomUUID(),
            )
        println(nettoPdfPayload)

        nettoPdfPayload shouldNotBe null
    }

    @Test
    fun testBruttoPdf() {
        val bruttoPdfPayload =
            PdfService(søknadRepository, seksjonRepository).genererBruttoPdfPayload(
                "987654321",
                randomUUID(),
            )
        println(bruttoPdfPayload)

        bruttoPdfPayload shouldNotBe null
    }
}
