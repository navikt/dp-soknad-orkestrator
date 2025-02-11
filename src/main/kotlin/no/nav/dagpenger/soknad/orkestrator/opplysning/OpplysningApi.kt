package no.nav.dagpenger.soknad.orkestrator.opplysning

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import no.nav.dagpenger.soknad.orkestrator.api.auth.AuthFactory.azureAd

internal fun Application.opplysningApi() {
    install(Authentication) {
        jwt("azureAd") { azureAd() }
    }

    routing {
        get("/") { call.respond(HttpStatusCode.OK) }

        authenticate("azureAd") {
            route("/opplysning") {
                get {
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}
