package no.nav.dagpenger.soknad.orkestrator.opplysning

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import no.nav.dagpenger.soknad.orkestrator.land.Landfabrikk
import no.nav.dagpenger.soknad.orkestrator.land.Landgruppe

internal fun Application.landApi() {
    routing {
        route("/land") {
            post("/oppslag") {
                val landgrupper: List<Landgruppe> =
                    try {
                        call.receive<List<Landgruppe>>()
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, "Kunne ikke parse request body til liste med landgrupper")
                        return@post
                    }

                val landgruppeDTOer = Landfabrikk.tilLandgruppeDTO(landgrupper)
                call.respond(HttpStatusCode.OK, landgruppeDTOer)
            }
        }
    }
}
