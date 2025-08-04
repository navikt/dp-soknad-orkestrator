package no.nav.dagpenger.soknad.orkestrator.personalia

import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import no.nav.dagpenger.soknad.orkestrator.api.auth.ident
import no.nav.dagpenger.soknad.orkestrator.api.auth.jwt

internal fun Application.personaliaApi(personaliaService: PersonaliaService) {
    routing {
        authenticate("tokenX") {
            route("/personalia") {
                get {
                    call.respond(
                        personaliaService.hentPersonalia(call.ident(), call.request.jwt()),
                    )
                }
            }
        }
    }
}
