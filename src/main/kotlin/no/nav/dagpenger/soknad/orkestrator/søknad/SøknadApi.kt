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
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.util.pipeline.PipelineContext
import no.nav.dagpenger.soknad.orkestrator.api.auth.ident
import no.nav.dagpenger.soknad.orkestrator.opplysning.Svar
import java.util.UUID

internal fun Application.søknadApi(søknadService: SøknadService) {
    routing {
        authenticate("tokenX") {
            route("/soknad") {
                post("/start") {
                    val søknad = søknadService.hentEllerOpprettSøknad(call.ident())

                    call.respond(HttpStatusCode.OK, søknad.søknadId)
                }

                get("/{søknadId}/neste") {
                    val søknadId = søknadId()

                    val søknad = søknadService.hentSøknad(søknadId)

                    call.respond(HttpStatusCode.OK, søknad)
                }

                put("/{søknadId}/svar") {
                    val søknadId = søknadId()

                    val svar =
                        try {
                            call.receive<Svar<*>>()
                        } catch (e: Exception) {
                            throw IllegalArgumentException("Kunne ikke parse svar", e)
                        }

                    søknadService.håndterSvar(søknadId = søknadId, svar)

                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}

internal fun PipelineContext<Unit, ApplicationCall>.søknadId() =
    call.parameters["søknadId"].let { UUID.fromString(it) }
        ?: throw IllegalArgumentException("Må ha med id i parameter")
