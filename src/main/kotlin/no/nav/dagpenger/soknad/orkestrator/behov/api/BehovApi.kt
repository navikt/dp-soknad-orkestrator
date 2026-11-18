package no.nav.dagpenger.soknad.orkestrator.behov.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing

internal fun Application.behovApi() {
    routing {
        authenticate("azureAd-maskin") {
            post("/behov") {
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}
