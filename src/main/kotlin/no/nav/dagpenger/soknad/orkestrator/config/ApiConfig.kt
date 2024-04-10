package no.nav.dagpenger.soknad.orkestrator.config

import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.request.path
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import org.slf4j.event.Level

internal fun Application.apiKonfigurasjon() {
    install(CallLogging) {
        disableDefaultColors()
        filter {
            it.request.path() !in setOf("/metrics", "/isalive", "/isready")
        }
        level = Level.INFO
    }

    val micrometerRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    routing {
        get("/metrics") {
            call.respond(micrometerRegistry.scrape())
        }
    }
}
