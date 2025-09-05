package no.nav.dagpenger.soknad.orkestrator.opplysning

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import no.nav.dagpenger.soknad.orkestrator.api.auth.saksbehandlerId
import no.nav.dagpenger.soknad.orkestrator.api.models.OppdatertBarnRequestDTO
import no.nav.dagpenger.soknad.orkestrator.metrikker.OpplysningMetrikker
import java.util.UUID

internal fun Application.opplysningApi(opplysningService: OpplysningService) {
    routing {
        get("/") { call.respond(HttpStatusCode.OK) }

        authenticate("azureAd") {
            route("/opplysninger") {
                route("/barn/{soknadbarnId}") {
                    get {
                        val søknadbarnId = validerOgFormaterSøknadbarnIdParam() ?: return@get
                        val søknadId = opplysningService.mapTilSøknadId(søknadbarnId)

                        call.respond(HttpStatusCode.OK, opplysningService.hentBarn(søknadId))
                    }

                    put("/oppdater") {
                        val søknadbarnId = validerOgFormaterSøknadbarnIdParam() ?: return@put
                        val søknadId = opplysningService.mapTilSøknadId(søknadbarnId)

                        val oppdatertBarnRequest: OppdatertBarnRequestDTO
                        val token =
                            call.request.headers["Authorization"]?.removePrefix("Bearer ")
                                ?: throw IllegalArgumentException("Fant ikke token i request header")

                        try {
                            oppdatertBarnRequest = call.receive<OppdatertBarnRequestDTO>()
                        } catch (e: Exception) {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                "Kunne ikke parse request body til OppdatertBarnRequestDTO. Feilmelding: $e",
                            )
                            return@put
                        }

                        val oppdatertBarn = oppdatertBarnRequest.oppdatertBarn
                        val saksbehandlerId = call.saksbehandlerId()

                        if (opplysningService.hentBarn(søknadId).find { it.barnId == oppdatertBarn.barnId } == null) {
                            call.respond(
                                HttpStatusCode.NotFound,
                                "Fant ikke barn med id ${oppdatertBarn.barnId} for søknad $søknadId",
                            )
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
                            opplysningService.oppdaterBarn(oppdatertBarnRequest, søknadId, saksbehandlerId, token)
                            OpplysningMetrikker.endringBarn.inc()
                            call.respond(HttpStatusCode.OK)
                        } else {
                            call.respond(
                                HttpStatusCode.NotModified,
                                "Opplysningen inneholder ingen endringer, kan ikke oppdatere",
                            )
                            return@put
                        }
                    }
                }
            }
        }
    }
}

private suspend fun RoutingContext.validerOgFormaterSøknadbarnIdParam(): UUID? {
    val soknadbarnIdParam =
        call.parameters["soknadbarnId"] ?: run {
            call.respond(HttpStatusCode.BadRequest, "Mangler soknadbarnId i parameter")
            return null
        }

    return try {
        UUID.fromString(soknadbarnIdParam)
    } catch (e: Exception) {
        call.respond<String>(
            HttpStatusCode.BadRequest,
            "Kunne ikke parse soknadbarnId parameter $soknadbarnIdParam til UUID. Feilmelding: $e",
        )
        return null
    }
}
