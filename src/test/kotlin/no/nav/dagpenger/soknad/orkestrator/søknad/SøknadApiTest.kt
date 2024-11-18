package no.nav.dagpenger.soknad.orkestrator.søknad

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.navikt.tbd_libs.naisful.test.naisfulTestApp
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.mockk.mockk
import no.nav.dagpenger.soknad.orkestrator.api.models.OrkestratorSoknadDTO
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import no.nav.dagpenger.soknad.orkestrator.utils.TestApplication
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.UUID
import kotlin.test.Test

class SøknadApiTest {
    val søknadEndepunkt = "/soknad"
    val søknadId = UUID.randomUUID()
    val ident = "12345678901"
    val testToken by TestApplication

    val søknadService = mockk<SøknadService>(relaxed = true)

    @Test
    fun `Start-søknad svarer med en uuid`() {
        naisfulTestApp(
            testApplicationModule = { søknadApi(søknadService) },
            meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
            objectMapper = objectMapper,
        ) {
            client
                .post("$søknadEndepunkt/start") {
                    header(HttpHeaders.Authorization, "Bearer $testToken")
                }.let { respons ->
                    respons.status shouldBe HttpStatusCode.OK
                    shouldNotThrow<Exception> { objectMapper.readValue(respons.bodyAsText(), UUID::class.java) }
                }
        }
    }

    @Test
    fun `Uautentiserte kall responderer med Unauthorized`() {
        naisfulTestApp(
            testApplicationModule = { søknadApi(søknadService) },
            meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
            objectMapper = objectMapper,
        ) {
            client.post("$søknadEndepunkt/start").status shouldBe HttpStatusCode.Unauthorized
        }
    }

    @Test
    fun `Returnerer neste seksjon for en gitt søknadId`() =
        naisfulTestApp(
            testApplicationModule = { søknadApi(søknadService) },
            meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
            objectMapper = objectMapper,
        ) {
            client
                .get("$søknadEndepunkt/$søknadId/neste") {
                    header(HttpHeaders.Authorization, "Bearer $testToken")
                }.let { respons ->
                    respons.status shouldBe HttpStatusCode.OK
                    shouldNotThrow<Exception> {
                        objectMapper.readValue<OrkestratorSoknadDTO>(respons.bodyAsText())
                    }
                }
        }

    @ParameterizedTest
    @MethodSource("alleSvartyperSomJson")
    fun `Kan besvare en opplysning`(jsonSvar: String) {
        naisfulTestApp(
            testApplicationModule = { søknadApi(søknadService) },
            meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
            objectMapper = objectMapper,
        ) {
            client
                .put("$søknadEndepunkt/$søknadId/svar") {
                    header(HttpHeaders.Authorization, "Bearer $testToken")
                    contentType(ContentType.Application.Json)
                    setBody(jsonSvar)
                }.let { respons ->
                    respons.status shouldBe HttpStatusCode.OK
                }
        }
    }

    @ParameterizedTest
    @MethodSource("svarMedFeilVerditype")
    fun `Kan ikke besvare en opplysning hvor type og verdi ikke samsvarer`(jsonSvarMedFeilVerditype: String) {
        naisfulTestApp(
            testApplicationModule = { søknadApi(søknadService) },
            meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
            objectMapper = objectMapper,
        ) {
            val respons =
                client.put("$søknadEndepunkt/$søknadId/svar") {
                    header(HttpHeaders.Authorization, "Bearer $testToken")
                    contentType(ContentType.Application.Json)
                    setBody(jsonSvarMedFeilVerditype)
                }
            respons.status shouldBe HttpStatusCode.BadRequest
        }
    }

    @Test
    fun `Kan besvare opplysning når type er definert i lowercase`() {
        val jsonSvarMedLowercaseType =
            """
            {
              "opplysningId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
              "type": "boolean",
              "verdi": true
            }
            """.trimIndent()

        naisfulTestApp(
            testApplicationModule = { søknadApi(søknadService) },
            meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
            objectMapper = objectMapper,
        ) {
            client
                .put("$søknadEndepunkt/$søknadId/svar") {
                    header(HttpHeaders.Authorization, "Bearer $testToken")
                    contentType(ContentType.Application.Json)
                    setBody(jsonSvarMedLowercaseType)
                }.let { respons ->
                    respons.status shouldBe HttpStatusCode.OK
                }
        }
    }

    companion object {
        //language=JSON
        @JvmStatic
        fun alleSvartyperSomJson() =
            listOf(
                """
                {
                  "opplysningId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                  "type": "BOOLEAN",
                  "verdi": true
                }
                """.trimIndent(),
                """
                {
                  "opplysningId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                  "type": "PERIODE",
                  "verdi": {
                    "fom": "2022-01-01",
                    "tom": "2022-12-31"
                  }
                }
                """.trimIndent(),
                """
                {
                  "opplysningId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                  "type": "LAND",
                  "verdi": "NOR"
                }
                """.trimIndent(),
                """
                {
                  "opplysningId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                  "type": "TEKST",
                  "verdi": "Tekst svar"
                }
                """.trimIndent(),
                """
                {
                  "opplysningId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                  "type": "DATO",
                  "verdi": "2022-12-31"
                }
                """.trimIndent(),
            )

        //language=JSON
        @JvmStatic
        fun svarMedFeilVerditype() =
            listOf(
                """
                {
                  "opplysningId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                  "type": "BOOLEAN",
                  "verdi": "dette er ikke en boolean"
                }
                """.trimIndent(),
                """
                {
                  "opplysningId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                  "type": "PERIODE",
                  "verdi": "2022-12-31"
                }
                """.trimIndent(),
                """
                {
                  "opplysningId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                  "type": "LAND",
                  "verdi": true
                }
                """.trimIndent(),
                """
                {
                  "opplysningId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                  "type": "TEKST",
                  "verdi": {
                    "fom": "2022-01-01",
                    "tom": "2022-12-31"
                  }
                }
                """.trimIndent(),
                """
                {
                  "opplysningId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                  "type": "DATO",
                  "verdi": "dette er ikke en dato"
                }
                """.trimIndent(),
            )
    }
}
