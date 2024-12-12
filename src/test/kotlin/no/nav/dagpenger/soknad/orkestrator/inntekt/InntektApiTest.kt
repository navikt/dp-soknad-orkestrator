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
import no.nav.dagpenger.soknad.orkestrator.api.models.ForeleggingresultatDTO
import no.nav.dagpenger.soknad.orkestrator.api.models.MinsteinntektGrunnlagDTO
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import no.nav.dagpenger.soknad.orkestrator.utils.TestApplication
import java.util.UUID
import kotlin.test.Test

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
}
