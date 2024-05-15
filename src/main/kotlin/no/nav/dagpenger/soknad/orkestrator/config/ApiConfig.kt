package no.nav.dagpenger.soknad.orkestrator.config

import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.path
import no.nav.dagpenger.soknad.orkestrator.api.auth.AuthFactory.tokenX
import no.nav.dagpenger.soknad.orkestrator.config.Configuration.config
import org.slf4j.event.Level

internal fun Application.apiKonfigurasjon() {
    install(CallLogging) {
        disableDefaultColors()
        filter {
            it.request.path() !in setOf("/metrics", "/isalive", "/isready")
        }
        level = Level.INFO
    }

    install(Authentication) {
        jwt("tokenX") { tokenX() }
    }

    install(ContentNegotiation) {
        jackson()
    }
}
