package no.nav.dagpenger.soknad.orkestrator.opplysning

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import no.nav.dagpenger.soknad.orkestrator.land.Landfabrikk.alleLand

internal fun Application.landApi() {
    routing {
        route("/land") {
            get {
                call.respond(HttpStatusCode.OK, alleLand)
            }
        }
    }
}
