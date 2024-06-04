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
import no.nav.dagpenger.soknad.orkestrator.spørsmål.Spørsmål
import no.nav.dagpenger.soknad.orkestrator.spørsmål.SpørsmålDTO
import no.nav.dagpenger.soknad.orkestrator.spørsmål.grupper.mockSpørsmålgrupper
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

                    call.respond(HttpStatusCode.OK, mockSpørsmålgrupper.random())
                    /*val nesteSpørsmål = søknadService.nesteSpørsmål(søknadId)

                    if (nesteSpørsmål == null) {
                        call.respond(HttpStatusCode.NoContent)
                    } else {
                        call.respond(HttpStatusCode.OK, nesteSpørsmål.toDTO())
                    }*/
                }

                post("/{søknadId}/svar") {
                    val søknadId = søknadId()
                    val svar = call.receive<SpørsmålDTO<*>>()

                    call.respond(HttpStatusCode.OK)

                    /*søknadService.besvarSpørsmål(søknadId, svar)
                    call.respond(HttpStatusCode.OK)*/
                }
            }
        }
    }
}

fun Spørsmål<*>.toDTO(): SpørsmålDTO<*> {
    return SpørsmålDTO(
        tekstnøkkel = tekstnøkkel,
        id = id,
        type = type,
        svar = svar,
        gyldigeSvar = gyldigeSvar,
    )
}

internal fun PipelineContext<Unit, ApplicationCall>.søknadId() =
    call.parameters["søknadId"].let { UUID.fromString(it) }
        ?: throw IllegalArgumentException("Må ha med id i parameter")
