package no.nav.dagpenger.soknad.orkestrator.søknad.seksjon
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import java.util.UUID

internal fun Application.seksjonApi() {
    routing {
        authenticate("azureAd") {
            route("/{søknadId}/{seksjonId}") {
                put {
                    val søknadId = validerOgFormaterSøknadIdParam() ?: return@put

                    val seksjonId = validerSeksjonIdParam() ?: return@put

                    call.respond(HttpStatusCode.OK, "Søknad API is up and running")
                }
            }
        }
    }
}

private suspend fun RoutingContext.validerSeksjonIdParam(): String? {
    val seksjonId =
        call.parameters["seksjonId"] ?: run {
            call.respond(HttpStatusCode.BadRequest, "Mangler seksjonId i parameter")
            return null
        }

    return seksjonId
}

private suspend fun RoutingContext.validerOgFormaterSøknadIdParam(): UUID? {
    val søknadIdParam =
        call.parameters["søknadId"] ?: run {
            call.respond(HttpStatusCode.BadRequest, "Mangler søknadId i parameter")
            return null
        }

    return try {
        UUID.fromString(søknadIdParam)
    } catch (e: Exception) {
        call.respond<String>(
            HttpStatusCode.BadRequest,
            "Kunne ikke parse søknadId parameter $søknadIdParam til UUID. Feilmelding: $e",
        )
        return null
    }
}
