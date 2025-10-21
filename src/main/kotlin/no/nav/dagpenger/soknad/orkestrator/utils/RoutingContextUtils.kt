package no.nav.dagpenger.soknad.orkestrator.utils

import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext
import java.util.UUID

suspend fun RoutingContext.validerOgFormaterSøknadIdParam(): UUID? {
    val søknadIdParam =
        call.parameters["søknadId"] ?: run {
            call.respond(BadRequest, "Mangler søknadId i parameter")
            return null
        }

    return try {
        UUID.fromString(søknadIdParam)
    } catch (e: Exception) {
        call.respond<String>(
            BadRequest,
            "Kunne ikke parse søknadId parameter $søknadIdParam til UUID. Feilmelding: $e",
        )
        return null
    }
}

suspend fun RoutingContext.validerSeksjonIdParam(): String? {
    return run {
        call.parameters["seksjonId"] ?: run {
            call.respond(BadRequest, "Mangler seksjonId i parameter")
            return null
        }
    }
}
