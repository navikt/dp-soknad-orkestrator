package no.nav.dagpenger.soknad.orkestrator.opplysning

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
import no.nav.dagpenger.soknad.orkestrator.api.auth.AuthFactory.azureAd
import no.nav.dagpenger.soknad.orkestrator.api.models.OppdatertBarnRequestDTO
import java.util.UUID

internal fun Application.opplysningApi(opplysningService: OpplysningService) {
    install(Authentication) {
        jwt("azureAd") { azureAd() }
    }

    routing {
        get("/") { call.respond(HttpStatusCode.OK) }

        authenticate("azureAd") {
            route("/opplysninger/{søknadId}") {
                get {
                    call.respond(HttpStatusCode.OK)
                }
                route("/barn") {
                    get {
                        val søknadId = validerOgFormaterSøknadIdParam() ?: return@get

                        call.respond(HttpStatusCode.OK, opplysningService.hentBarn(søknadId))
                    }

                    put("/oppdater") {
                        val søknadId = validerOgFormaterSøknadIdParam() ?: return@put
                        val oppdatertBarn = call.receive<OppdatertBarnRequestDTO>()

                        if (opplysningService.hentBarn(søknadId).find { it.barnId == oppdatertBarn.barnId } == null) {
                            call.respond(HttpStatusCode.NotFound, "Fant ikke barn med id ${oppdatertBarn.barnId} for søknad $søknadId")
                            return@put
                        }

                        if (oppdatertBarn.kvalifisererTilBarnetillegg) {
                            if (oppdatertBarn.barnetilleggFom == null || oppdatertBarn.barnetilleggTom == null) {
                                call.respond(
                                    HttpStatusCode.BadRequest,
                                    "barnetilleggFom og barnetilleggTom må være satt når kvalifisererTilBarnetillegg er true",
                                )
                                return@put
                            }
                        }

                        if (opplysningService.erEndret(oppdatertBarn, søknadId)) {
                            // TODO: Oppdater barn
                            call.respond(HttpStatusCode.OK)
                        } else {
                            call.respond(HttpStatusCode.NotModified, "Opplysningen inneholder ingen endringer, kan ikke oppdatere")
                            return@put
                        }

                        call.respond(HttpStatusCode.OK)
                    }
                }
            }
        }
    }
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
