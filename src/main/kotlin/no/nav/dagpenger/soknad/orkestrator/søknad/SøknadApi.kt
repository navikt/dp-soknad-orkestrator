package no.nav.dagpenger.soknad.orkestrator.søknad

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import no.nav.dagpenger.soknad.orkestrator.api.auth.AuthFactory.tokenX
import no.nav.dagpenger.soknad.orkestrator.opplysning.Svar
import java.util.UUID

internal fun Application.søknadApi(søknadService: SøknadService) {
    install(Authentication) {
        jwt("tokenX") { tokenX() }
    }

    routing {
        get("/") { call.respond(HttpStatusCode.OK) }

        authenticate("tokenX") {
            route("/soknad") {
                get("/{søknadId}/neste") {
                    val søknadId = søknadUuid()

                    val søknad = søknadService.hentSøknad(søknadId)

                    call.respond(HttpStatusCode.OK, søknad)
                }

                put("/{søknadId}/svar") {
                    val søknadId = søknadUuid()

                    val svar =
                        try {
                            call.receive<Svar<*>>()
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.BadRequest, "Kunne ikke parse svar: ${e.message}")
                            return@put
                        }

                    søknadService.håndterSvar(søknadId = søknadId, svar)

                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}

private fun RoutingContext.søknadUuid() =
    (
        call.parameters["søknadId"].let { UUID.fromString(it) }
            ?: throw IllegalArgumentException("Må ha med søknadId i parameter")
    )
