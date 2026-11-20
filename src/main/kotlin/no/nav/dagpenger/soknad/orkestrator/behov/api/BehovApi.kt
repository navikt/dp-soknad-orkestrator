package no.nav.dagpenger.soknad.orkestrator.behov.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import no.nav.dagpenger.soknad.orkestrator.api.models.BehovRequestDTO
import no.nav.dagpenger.soknad.orkestrator.api.models.BehovResponseDTO
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov
import no.nav.dagpenger.soknad.orkestrator.behov.IkkeStøttetBehov
import no.nav.dagpenger.soknad.orkestrator.behov.SynkronBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.SøknadFinnesIkke

internal fun Application.behovApi(synkronBehovløser: SynkronBehovløser) {
    routing {
        authenticate("azureAd-maskin") {
            post("/behov/{behovNavn}") {
                val behov = Behov.entries.firstOrNull { it.name == call.parameters["behovNavn"] }
                when (behov) {
                    null -> {
                        call.respond(HttpStatusCode.BadRequest, "Ugyldig behov")
                    }

                    else -> {
                        val request = call.receive<BehovRequestDTO>()
                        try {
                            val verdi = synkronBehovløser.løs(behov, request.ident, request.søknadId)
                            call.respond(HttpStatusCode.OK, BehovResponseDTO(verdi = verdi))
                        } catch (_: IkkeStøttetBehov) {
                            call.respond(HttpStatusCode.BadRequest, "Ugyldig behov")
                        } catch (_: SøknadFinnesIkke) {
                            call.respond(HttpStatusCode.NotFound, "Fant ikke søknad")
                        }
                    }
                }
            }
        }
    }
}
