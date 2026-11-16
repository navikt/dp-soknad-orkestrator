package no.nav.dagpenger.soknad.orkestrator.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

internal fun Application.rotApi() {
    routing {
        get("/") {
            call.respond(HttpStatusCode.OK)
        }
    }
}
