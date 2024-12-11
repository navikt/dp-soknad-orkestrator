package no.nav.dagpenger.soknad.orkestrator.inntekt

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
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import no.nav.dagpenger.soknad.orkestrator.api.auth.AuthFactory.tokenX
import java.util.UUID

internal fun Application.inntektApi(inntektService: InntektService) {
    install(Authentication) {
        jwt("tokenX") { tokenX() }
    }

    routing {
        get("/") { call.respond(HttpStatusCode.OK) }

        authenticate("tokenX") {
            route("/inntekt/{søknadId}") {
                route("/minsteinntektGrunnlag") {
                    get {
                        val søknadId = søknadUuid()

                        val minsteinntektGrunnlag = inntektService.hentMinsteinntektGrunnlag(søknadId)

                        call.respond(HttpStatusCode.OK, minsteinntektGrunnlag)
                    }

                    post("/foreleggingresultat") {
                        val søknadId = søknadUuid()

                        inntektService.lagreSvar(søknadId, call.receive())

                        call.respond(HttpStatusCode.OK)
                    }
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
