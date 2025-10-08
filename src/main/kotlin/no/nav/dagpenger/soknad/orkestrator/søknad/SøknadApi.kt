package no.nav.dagpenger.soknad.orkestrator.søknad

import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion.Created
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import no.nav.dagpenger.soknad.orkestrator.api.auth.ident
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonService
import no.nav.dagpenger.soknad.orkestrator.utils.validerOgFormaterSøknadIdParam

internal fun Application.søknadApi(
    søknadService: SøknadService,
    seksjonService: SeksjonService,
) {
    routing {
        authenticate("tokenX") {
            route("/soknad") {
                post {
                    call.respondText(
                        status = Created,
                        text = søknadService.opprett(call.ident()).toString(),
                    )
                }
            }
            route("/soknad/{søknadId}") {
                post {
                    val søknadId =
                        validerOgFormaterSøknadIdParam() ?: return@post call.respond(HttpStatusCode.BadRequest)

                    call.respondText(
                        status = OK,
                        text = søknadService.sendInn(søknadId, call.ident()).toString(),
                    )
                }
            }
            route("/soknad/{søknadId}/progress") {
                get {
                    val søknadId =
                        validerOgFormaterSøknadIdParam() ?: return@get call.respond(HttpStatusCode.BadRequest)

                    val progress =
                        seksjonService.hentLagredeSeksjonerForGittSøknadId(søknadId)

                    if (progress.isEmpty()) {
                        return@get call.respond(NotFound, mapOf("seksjoner" to progress))
                    }

                    call.respond(OK, mapOf("seksjoner" to progress))
                }
            }
        }
    }
}
