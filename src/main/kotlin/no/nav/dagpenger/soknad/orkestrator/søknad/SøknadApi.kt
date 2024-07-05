package no.nav.dagpenger.soknad.orkestrator.søknad

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.util.pipeline.PipelineContext
import no.nav.dagpenger.soknad.orkestrator.api.auth.ident
import no.nav.dagpenger.soknad.orkestrator.api.models.SporsmalDTO
import java.util.UUID

internal fun Application.søknadApi(søknadService: SøknadService) {
    routing {
        authenticate("tokenX") {
            route("/soknad") {
                post("/start") {
                    val søknad = søknadService.opprettSøknad(call.ident())

                    call.respond(HttpStatusCode.Created, søknad.søknadId)
                }

                get("/{søknadId}/neste") {
                    val søknadId = søknadId()

                    val nesteSpørsmålgruppe = søknadService.nesteSpørsmålgruppe(søknadId)

                    call.respond(HttpStatusCode.OK, nesteSpørsmålgruppe)
                }

                post("/{søknadId}/svar") {
                    val søknadId = søknadId()
                    val besvartSpørsmål = call.receive<SporsmalDTO>()

                    søknadService.lagreBesvartSpørsmål(søknadId = søknadId, besvartSpørsmål = besvartSpørsmål)

                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}

internal fun PipelineContext<Unit, ApplicationCall>.søknadId() =
    call.parameters["søknadId"].let { UUID.fromString(it) }
        ?: throw IllegalArgumentException("Må ha med id i parameter")
