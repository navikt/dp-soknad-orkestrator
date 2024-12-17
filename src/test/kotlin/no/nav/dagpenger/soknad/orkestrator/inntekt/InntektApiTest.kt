package no.nav.dagpenger.soknad.orkestrator.inntekt

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.navikt.tbd_libs.naisful.test.naisfulTestApp
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.mockk.mockk
import no.nav.dagpenger.soknad.orkestrator.api.models.ForeleggingresultatDTO
import no.nav.dagpenger.soknad.orkestrator.api.models.MinsteinntektGrunnlagDTO
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import no.nav.dagpenger.soknad.orkestrator.utils.TestApplication
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertTrue

class InntektApiTest {
    val inntektService = mockk<InntektService>(relaxed = true)
    val søknadId = UUID.randomUUID()
    val minsteinntektEndepunkt = "/inntekt/$søknadId/minsteinntektGrunnlag"
    val testToken by TestApplication

    @Test
    fun `Uautentiserte kall returnerer 401`() {
        naisfulTestApp(
            testApplicationModule = { inntektApi(inntektService) },
            meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
            objectMapper = objectMapper,
        ) {
            client.get(minsteinntektEndepunkt).status shouldBe HttpStatusCode.Unauthorized
        }
    }

    @Test
    fun `Hent minsteinntektGrunnlag for en gitt søknadId returnerer 200 OK og minsteinntektGrunnlag`() {
        naisfulTestApp(
            testApplicationModule = { inntektApi(inntektService) },
            meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
            objectMapper = objectMapper,
        ) {
            client
                .get(minsteinntektEndepunkt) {
                    header(HttpHeaders.Authorization, "Bearer $testToken")
                }.let { respons ->
                    respons.status shouldBe HttpStatusCode.OK
                    shouldNotThrow<Exception> {
                        objectMapper.readValue<MinsteinntektGrunnlagDTO>(respons.bodyAsText())
                    }
                }
        }
    }

    @Test
    fun `Post forelegging resultat returnerer 200 OK`() {
        //language=JSON
        val foreleggingResultat =
            """
            {
              "søknadId": "$søknadId",
              "bekreftet": false,
              "begrunnelse": "Begrunnelse"
            }
            """.trimIndent()

        naisfulTestApp(
            testApplicationModule = { inntektApi(inntektService) },
            meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
            objectMapper = objectMapper,
        ) {
            client
                .post("$minsteinntektEndepunkt/foreleggingresultat") {
                    header(HttpHeaders.Authorization, "Bearer $testToken")
                    contentType(ContentType.Application.Json)
                    setBody(foreleggingResultat)
                }.let { respons ->
                    respons.status shouldBe HttpStatusCode.OK
                }
        }
    }

    @Test
    fun `Post pdf returnerer 200 OK`() {
        //language=JSON
        val htmlDokument =
            """
            {
              "html": "<html><body><h1>Hei</h1></body></html>"
            }
            """.trimIndent()

        naisfulTestApp(
            testApplicationModule = { inntektApi(inntektService) },
            meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
            objectMapper = objectMapper,
        ) {
            client
                .post("$minsteinntektEndepunkt/foreleggingresultat/journalforing") {
                    header(HttpHeaders.Authorization, "Bearer $testToken")
                    contentType(ContentType.Application.Json)
                    setBody(htmlDokument)
                }.let { respons ->
                    respons.status shouldBe HttpStatusCode.OK
                }
        }
    }

    @Test
    fun `Get foreleggingresultat returnerer 200 OK med body`() {
        naisfulTestApp(
            testApplicationModule = { inntektApi(inntektService) },
            meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
            objectMapper = objectMapper,
        ) {
            client
                .get("$minsteinntektEndepunkt/foreleggingresultat") {
                    header(HttpHeaders.Authorization, "Bearer $testToken")
                }.let { respons ->
                    respons.status shouldBe HttpStatusCode.OK
                    shouldNotThrow<Exception> {
                        objectMapper.readValue<ForeleggingresultatDTO>(respons.bodyAsText())
                    }
                }
        }
    }

    @Test
    fun testGeneratePdfFromHtml() {
        // Define a regular expression to find self-closing tags that are not properly closed
        val selfClosingTags = listOf("br", "img", "input", "hr", "meta", "link")
        val pattern = Regex("<(${selfClosingTags.joinToString("|")})([^>]*)>")

        // Replace the tags with properly closed self-closing tags
        val correctedHtmlContent =
            pattern.replace(html) { matchResult ->
                "<${matchResult.groupValues[1]}${matchResult.groupValues[2]} />"
            }

        val pdfBytes = generatePdfFromHtml(correctedHtmlContent)
        val pdfFile = File("test-output/test.pdf")
        pdfFile.parentFile.mkdirs()
        pdfFile.writeBytes(pdfBytes)

        assertTrue(pdfFile.exists(), "PDF file should be created")
    }
}

fun generatePdfFromHtml(html: String): ByteArray {
    val outputStream = ByteArrayOutputStream()
    val builder = PdfRendererBuilder()
    builder.withHtmlContent(html, null)
    builder.toStream(outputStream)
    builder.run()
    return outputStream.toByteArray()
}

//language=HTML
val html =
    """
        <div class="card"><span
            class="navds-tag _tag_1r3cn_1 tag--pdf navds-tag--neutral-filled navds-tag--medium navds-body-short navds-body-short--medium">Hentet fra Skatteetaten</span>
        <h2 class="navds-heading navds-heading--medium navds-typo--spacing">Inntekt</h2>
        <p class="navds-body-long navds-body-long--medium navds-typo--spacing">Inntekt siste 12 måneder fra <!-- -->17.
            desember 2023<!-- --> til <!-- -->17. desember 2024<!-- --> <br><strong>100000<!-- --> kroner</strong></p>
        <p class="navds-body-long navds-body-long--medium navds-typo--spacing">Inntekt siste 36 måneder fra <!-- -->17.
            desember 2021<!-- --> til <!-- -->17. desember 2024<!-- --> <br><strong>200000<!-- --> kroner</strong>.</p>
        <div class="_verticalLine_1r3cn_5" aria-hidden="true"></div>
        <div class="navds-read-more navds-read-more--medium readmore--pdf">
            <button type="button" class="navds-read-more__button navds-body-short" aria-expanded="false"
                    data-state="closed">
                <svg xmlns="http://www.w3.org/2000/svg" width="1em" height="1em" fill="none" viewBox="0 0 24 24"
                     focusable="false" role="img" class="navds-read-more__expand-icon" aria-hidden="true">
                    <path fill="currentColor" fill-rule="evenodd"
                          d="M5.97 9.47a.75.75 0 0 1 1.06 0L12 14.44l4.97-4.97a.75.75 0 1 1 1.06 1.06l-5.5 5.5a.75.75 0 0 1-1.06 0l-5.5-5.5a.75.75 0 0 1 0-1.06"
                          clip-rule="evenodd"></path>
                </svg>
                <span>Hvilke inntekter gir rett til dagpenger?</span></button>
            <div aria-hidden="true" data-state="closed"
                 class="navds-read-more__content navds-read-more__content--closed navds-body-long navds-body-long--medium">
                <p>Vi bruker <strong>disse inntektene</strong> for å finne ut om du har rett til dagpenger:</p>
                <ul>
                    <li>Arbeidsinntekt</li>
                    <li>Foreldrepenger som arbeidstaker</li>
                    <li>Svangerskapspenger som arbeidstaker</li>
                    <li>Svangerskapsrelaterte sykepenger som arbeidstaker</li>
                </ul>
                <p><strong>Inntekt som selvstendig næringsdrivende</strong> regnes ikke som arbeidsinntekt.</p>
                <p>Vi har hentet arbeidsinntekten din fra Skatteetaten de siste 12 månedene og 36 månedene. NAV velger det
                    alternativet som er best for deg når vi vurderer om du har rett til dagpenger.</p></div>
        </div>
    </div>
    """.trimIndent()
