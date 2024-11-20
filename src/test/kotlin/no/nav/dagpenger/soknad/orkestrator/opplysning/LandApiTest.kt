package no.nav.dagpenger.soknad.orkestrator.opplysning

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.navikt.tbd_libs.naisful.test.naisfulTestApp
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.matchers.shouldBe
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.dagpenger.soknad.orkestrator.api.models.LandgruppeDTO
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import kotlin.test.Test

class LandApiTest {
    @Test
    fun `Landoppslag svarer med 200 OK ved gyldig landgruppe i request body`() {
        naisfulTestApp(
            testApplicationModule = { landApi() },
            meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
            objectMapper = objectMapper,
        ) {
            val respons =
                client.post("/land/oppslag") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json)
                    setBody(listOf(Landgruppe.NORGE))
                }

            respons.status shouldBe HttpStatusCode.OK
            shouldNotThrow<IllegalArgumentException> {
                objectMapper.readValue<List<LandgruppeDTO>>(respons.bodyAsText())
            }
        }
    }

    @Test
    fun `Landoppslag svarer med 400 Bad Request ved ugyldig landgruppe i request body`() {
        naisfulTestApp(
            testApplicationModule = { landApi() },
            meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
            objectMapper = objectMapper,
        ) {
            val respons =
                client.post("/land/oppslag") {
                    header(HttpHeaders.ContentType, ContentType.Application.Json)
                    setBody(listOf("UGYLDIG_LANDGRUPPE"))
                }

            respons.status shouldBe HttpStatusCode.BadRequest
            respons.bodyAsText() shouldBe "Kunne ikke parse request body til liste med landgrupper"
        }
    }
}
