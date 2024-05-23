package no.nav.dagpenger.soknad.orkestrator.søknad

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import java.util.UUID

internal fun Application.søknadApi() {
    routing {
        authenticate("tokenX") {
            route("/start-soknad") {
                post { call.respond(HttpStatusCode.OK, UUID.randomUUID()) }
            }
        }
    }
}
