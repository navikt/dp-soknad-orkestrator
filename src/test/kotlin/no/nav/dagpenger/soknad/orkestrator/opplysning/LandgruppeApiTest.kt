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
import no.nav.dagpenger.soknad.orkestrator.api.models.LandgruppeDTO
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import kotlin.test.Test

class LandgruppeApiTest {
    @Test
    fun `Landgrupper svarer med 200 OK og en liste med LandgruppeDTO`() {
        naisfulTestApp(
            testApplicationModule = { landgruppeApi() },
            meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
            objectMapper = objectMapper,
        ) {
            val respons = client.get("/landgrupper")

            respons.status shouldBe HttpStatusCode.OK
            shouldNotThrow<IllegalArgumentException> {
                objectMapper.readValue<List<LandgruppeDTO>>(respons.bodyAsText())
            }
        }
    }
}
