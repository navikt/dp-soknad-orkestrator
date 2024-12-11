package no.nav.dagpenger.soknad.orkestrator.inntekt

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.navikt.tbd_libs.naisful.test.naisfulTestApp
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
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import no.nav.dagpenger.soknad.orkestrator.utils.TestApplication
import org.junit.jupiter.api.Disabled
import java.util.UUID
import kotlin.test.Test

class InntektApiTest {
    val minsteinntektEndepunkt = "/inntekt/minsteinntektGrunnlag"
    val inntektService = mockk<InntektService>(relaxed = true)
    val søknadId = UUID.randomUUID()
    val testToken by TestApplication

    @Test
    fun `Uautentiserte kall returnerer 401`() {
        naisfulTestApp(
            testApplicationModule = { inntektApi(inntektService) },
            meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
            objectMapper = objectMapper,
        ) {
            client.get("$minsteinntektEndepunkt/$søknadId").status shouldBe HttpStatusCode.Unauthorized
        }
    }

    @Test
    fun `Hent minsteinntektGrunnlag for en gitt søknadId returnerer 200 OK og minsteinntektGrunnlag`() {
        naisfulTestApp(
            testApplicationModule = { inntektApi(inntektService) },
            meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
            objectMapper = objectMapper,
        ) {
            client.get("$minsteinntektEndepunkt/$søknadId") {
                header(HttpHeaders.Authorization, "Bearer $testToken")
            }.let { respons ->
                respons.status shouldBe HttpStatusCode.OK
                shouldNotThrow<Exception> {
                    objectMapper.readValue<MinsteinntektGrunnlag>(respons.bodyAsText())
                }

                println("PRINT:" + objectMapper.readValue<MinsteinntektGrunnlag>(respons.bodyAsText()))
            }
        }
    }

    @Disabled
    @Test
    fun `Post forelegging resultat returnerer 200 OK`() {
        //language=JSON
        val foreleggingResultat =
            """
            {
              "minsteinntektGrunnlag": {
                "siste12mnd": "100000",
                "siste36mnd": "200000"
              }
            }
            """.trimIndent()

        naisfulTestApp(
            testApplicationModule = { inntektApi(inntektService) },
            meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
            objectMapper = objectMapper,
        ) {
            client.post("$minsteinntektEndepunkt/foreleggingResultat/$søknadId") {
                header(HttpHeaders.Authorization, "Bearer $testToken")
                contentType(ContentType.Application.Json)
                setBody(foreleggingResultat)
            }.let { respons ->
                respons.status shouldBe HttpStatusCode.OK
            }
        }
    }
}
