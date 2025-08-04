package no.nav.dagpenger.soknad.orkestrator.barn

import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import no.nav.dagpenger.soknad.orkestrator.api.auth.ident

internal fun Application.barnApi(barnService: BarnService) {
    routing {
        authenticate("tokenX") {
            route("/barn") {
                get {
                    call.respond(
                        barnService.hentBarn(call.ident()),
                    )
                }
            }
        }
    }
}
