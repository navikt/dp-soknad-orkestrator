package no.nav.dagpenger.soknad.orkestrator.opplysning

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.navikt.tbd_libs.naisful.test.naisfulTestApp
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.dagpenger.soknad.orkestrator.api.models.LandDTO
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import kotlin.test.Test

class LandApiTest {
    @Test
    fun `Land svarer med 200 OK og en liste med Land`() {
        naisfulTestApp(
            testApplicationModule = { landApi() },
            meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
            objectMapper = objectMapper,
        ) {
            val respons = client.get("/land")

            respons.status shouldBe HttpStatusCode.OK
            shouldNotThrow<IllegalArgumentException> {
                objectMapper.readValue<List<LandDTO>>(respons.bodyAsText())
            }
        }
    }
}
