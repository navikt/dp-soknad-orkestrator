package no.nav.dagpenger.soknad.orkestrator.opplysning

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import no.nav.dagpenger.soknad.orkestrator.api.auth.saksbehandlerId
import no.nav.dagpenger.soknad.orkestrator.api.models.NyttBarnRequestDTO
import no.nav.dagpenger.soknad.orkestrator.api.models.OppdatertBarnRequestDTO
import no.nav.dagpenger.soknad.orkestrator.metrikker.OpplysningMetrikker
import java.util.UUID

private val sikkerlogg = KotlinLogging.logger("tjenestekall.OpplysningApi")

internal fun Application.opplysningApi(opplysningService: OpplysningService) {
    routing {
        get("/") { call.respond(HttpStatusCode.OK) }

        authenticate("azureAd") {
            route("/opplysninger") {
                route("/{soknadId}") {
                    route("/barn") {
                        val parameternavn = "soknadId"
                        get {
                            val søknadId = validerOgFormaterUuidParameter(parameternavn) ?: return@get

                            call.respond(HttpStatusCode.OK, opplysningService.hentBarn(søknadId))
                        }

                        put("/oppdater") {
                            val søknadId = validerOgFormaterUuidParameter(parameternavn) ?: return@put

                            oppdaterBarn(opplysningService, søknadId)
                        }
                    }
                }
                route("/barn/{soknadbarnId}") {
                    val parameternavn = "soknadbarnId"
                    get {
                        val søknadbarnId = validerOgFormaterUuidParameter(parameternavn) ?: return@get
                        val søknadId = opplysningService.mapTilSøknadId(søknadbarnId)

                        val barn = opplysningService.hentBarn(søknadId)
                        sikkerlogg.info { "GET /barn/$søknadbarnId: søknadId=$søknadId, returnerer ${barn.size} barn: $barn" }
                        call.respond(HttpStatusCode.OK, barn)
                    }

                    put {
                        val søknadbarnId = validerOgFormaterUuidParameter(parameternavn) ?: return@put
                        val søknadId = opplysningService.mapTilSøknadId(søknadbarnId)

                        sikkerlogg.info { "PUT /barn/$søknadbarnId: søknadId=$søknadId" }
                        oppdaterBarn(opplysningService, søknadId)
                    }

                    post {
                        val søknadbarnId = validerOgFormaterUuidParameter(parameternavn) ?: return@post
                        val søknadId = opplysningService.mapTilSøknadId(søknadbarnId)

                        sikkerlogg.info { "POST /barn/$søknadbarnId: søknadId=$søknadId" }
                        leggTilBarn(opplysningService, søknadId)
                    }
                }
            }
        }
    }
}

private suspend fun RoutingContext.leggTilBarn(
    opplysningService: OpplysningService,
    søknadId: UUID,
) {
    val token =
        call.request.headers["Authorization"]?.removePrefix("Bearer ")
            ?: throw IllegalArgumentException("Fant ikke token i request header")

    val nyttBarnRequest: NyttBarnRequestDTO
    try {
        nyttBarnRequest = call.receive<NyttBarnRequestDTO>()
    } catch (e: Exception) {
        sikkerlogg.error(e) { "POST /barn leggTilBarn: Kunne ikke parse request body" }
        call.respond(
            HttpStatusCode.BadRequest,
            "Kunne ikke parse request body til NyttBarnRequestDTO. Feilmelding: $e",
        )
        return
    }

    val saksbehandlerId = call.saksbehandlerId()
    sikkerlogg.info { "POST /barn leggTilBarn: saksbehandlerId=$saksbehandlerId, request=$nyttBarnRequest" }
    val barnListe = opplysningService.leggTilBarn(nyttBarnRequest, søknadId, saksbehandlerId, token)
    call.respond(HttpStatusCode.Created, barnListe)
}

private suspend fun RoutingContext.oppdaterBarn(
    opplysningService: OpplysningService,
    søknadId: UUID,
) {
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
        return
    }

    val oppdatertBarn = oppdatertBarnRequest.oppdatertBarn
    val saksbehandlerId = call.saksbehandlerId()
    sikkerlogg.info { "PUT /barn oppdaterBarn: saksbehandlerId=$saksbehandlerId, request=$oppdatertBarnRequest" }

    if (opplysningService.hentBarn(søknadId).none { it.barnId == oppdatertBarn.barnId }) {
        call.respond(
            HttpStatusCode.NotFound,
            "Fant ikke barn med id ${oppdatertBarn.barnId} for søknad $søknadId",
        )
        return
    }

    if (oppdatertBarn.kvalifisererTilBarnetillegg && (oppdatertBarn.barnetilleggFom == null || oppdatertBarn.barnetilleggTom == null)) {
        call.respond(
            HttpStatusCode.BadRequest,
            "barnetilleggFom og barnetilleggTom må være satt når kvalifisererTilBarnetillegg er true",
        )
        return
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
        return
    }
}

private suspend fun RoutingContext.validerOgFormaterUuidParameter(parameternavn: String): UUID? {
    val parameterverdi =
        call.parameters[parameternavn] ?: run {
            call.respond(HttpStatusCode.BadRequest, "Mangler $parameternavn i requesten")
            return null
        }

    return try {
        UUID.fromString(parameterverdi)
    } catch (e: Exception) {
        call.respond<String>(
            HttpStatusCode.BadRequest,
            "Kunne ikke parse $parameternavn parameter $parameterverdi til UUID. Feilmelding: $e",
        )
        return null
    }
}
