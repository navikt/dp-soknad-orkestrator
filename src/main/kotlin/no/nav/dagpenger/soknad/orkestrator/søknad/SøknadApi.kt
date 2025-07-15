package no.nav.dagpenger.soknad.orkestrator.søknad

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import no.nav.dagpenger.soknad.orkestrator.api.auth.ident

internal fun Application.søknadApi(søknadService: SøknadService) {
    routing {
        authenticate("tokenX") {
            route("/soknad") {
                post {
                    call.respond(HttpStatusCode.Created, søknadService.opprett(call.ident()))
                }
            }
        }
    }
}
