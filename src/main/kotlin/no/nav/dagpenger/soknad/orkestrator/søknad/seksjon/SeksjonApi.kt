package no.nav.dagpenger.soknad.orkestrator.søknad.seksjon

import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import no.nav.dagpenger.soknad.orkestrator.utils.validerOgFormaterSøknadIdParam

internal fun Application.seksjonApi(seksjonService: SeksjonService) {
    routing {
        authenticate("tokenX") {
            route("/seksjon/{søknadId}/{seksjonId}") {
                put {
                    val søknadId = validerOgFormaterSøknadIdParam() ?: return@put
                    val seksjonId = validerSeksjonIdParam() ?: return@put
                    seksjonService.lagre(søknadId, seksjonId, call.receive<String>())
                    call.respond(OK, "Søknad API is up and running")
                }

                get {
                    val søknadId = validerOgFormaterSøknadIdParam() ?: return@get
                    val seksjonId = validerSeksjonIdParam() ?: return@get

                    val seksjon =
                        seksjonService.hent(søknadId, seksjonId)
                            ?: run {
                                call.respond(NotFound, "Fant ikke seksjon med id $seksjonId for søknad $søknadId")
                                return@get
                            }

                    call.respond(OK, seksjon)
                }
            }
            route("/seksjon/{søknadId}") {
                get {
                    val søknadId = validerOgFormaterSøknadIdParam() ?: return@get

                    val seksjoner = seksjonService.hentAlle(søknadId)

                    if (seksjoner.isEmpty()) {
                        call.respond(NotFound, "Fant ingen seksjoner for søknad $søknadId")
                        return@get
                    }

                    call.respond(OK, seksjoner)
                }
            }
        }
    }
}

private suspend fun RoutingContext.validerSeksjonIdParam(): String? {
    val seksjonId =
        call.parameters["seksjonId"] ?: run {
            call.respond(BadRequest, "Mangler seksjonId i parameter")
            return null
        }

    return seksjonId
}
