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
import no.nav.dagpenger.soknad.orkestrator.api.models.BarnRequestDTO
import no.nav.dagpenger.soknad.orkestrator.api.models.SlettBarnRequestDTO
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

                            oppdaterBarnDeprecated(opplysningService, søknadId)
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

                    post {
                        val søknadbarnId = validerOgFormaterUuidParameter(parameternavn) ?: return@post
                        val søknadId = opplysningService.mapTilSøknadId(søknadbarnId)

                        sikkerlogg.info { "POST /barn/$søknadbarnId: søknadId=$søknadId" }
                        leggTilBarn(opplysningService, søknadId)
                    }

                    route("/{barnId}") {
                        put {
                            val søknadbarnId = validerOgFormaterUuidParameter(parameternavn) ?: return@put
                            val barnId = validerOgFormaterUuidParameter("barnId") ?: return@put
                            val søknadId = opplysningService.mapTilSøknadId(søknadbarnId)

                            sikkerlogg.info { "PUT /barn/$søknadbarnId/$barnId: søknadId=$søknadId" }
                            oppdaterBarn(opplysningService, søknadId, barnId)
                        }

                        post("/slett") {
                            val søknadbarnId = validerOgFormaterUuidParameter(parameternavn) ?: return@post
                            val barnId = validerOgFormaterUuidParameter("barnId") ?: return@post
                            val søknadId = opplysningService.mapTilSøknadId(søknadbarnId)

                            sikkerlogg.info { "POST /barn/$søknadbarnId/$barnId/slett: søknadId=$søknadId" }
                            slettBarn(opplysningService, søknadId, barnId)
                        }
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

    val barnRequest: BarnRequestDTO
    try {
        barnRequest = call.receive<BarnRequestDTO>()
    } catch (e: Exception) {
        sikkerlogg.error(e) { "POST /barn leggTilBarn: Kunne ikke parse request body" }
        call.respond(
            HttpStatusCode.BadRequest,
            "Kunne ikke parse request body til BarnRequestDTO. Feilmelding: $e",
        )
        return
    }

    val saksbehandlerId = call.saksbehandlerId()
    sikkerlogg.info { "POST /barn leggTilBarn: saksbehandlerId=$saksbehandlerId, request=$barnRequest" }
    val barnListe = opplysningService.leggTilBarn(barnRequest, søknadId, saksbehandlerId, token)
    call.respond(HttpStatusCode.Created, barnListe)
}

private suspend fun RoutingContext.oppdaterBarn(
    opplysningService: OpplysningService,
    søknadId: UUID,
    barnId: UUID,
) {
    val barnRequest: BarnRequestDTO
    val token =
        call.request.headers["Authorization"]?.removePrefix("Bearer ")
            ?: throw IllegalArgumentException("Fant ikke token i request header")

    try {
        barnRequest = call.receive<BarnRequestDTO>()
    } catch (e: Exception) {
        call.respond(
            HttpStatusCode.BadRequest,
            "Kunne ikke parse request body til BarnRequestDTO. Feilmelding: $e",
        )
        return
    }

    val barn = barnRequest.barn
    val saksbehandlerId = call.saksbehandlerId()
    sikkerlogg.info { "PUT /barn oppdaterBarn: saksbehandlerId=$saksbehandlerId, barnId=$barnId, request=$barnRequest" }

    if (opplysningService.hentBarn(søknadId).none { it.barnId == barnId }) {
        call.respond(
            HttpStatusCode.NotFound,
            "Fant ikke barn med id $barnId for søknad $søknadId",
        )
        return
    }

    if (barn.kvalifisererTilBarnetillegg && (barn.barnetilleggFom == null || barn.barnetilleggTom == null)) {
        call.respond(
            HttpStatusCode.BadRequest,
            "barnetilleggFom og barnetilleggTom må være satt når kvalifisererTilBarnetillegg er true",
        )
        return
    }

    val fom = barn.barnetilleggFom
    val tom = barn.barnetilleggTom
    if (fom != null && tom != null && fom > tom) {
        call.respond(
            HttpStatusCode.BadRequest,
            "barnetilleggTom kan ikke være før barnetilleggFom",
        )
        return
    }

    if (opplysningService.erEndret(barn, barnId, søknadId)) {
        opplysningService.oppdaterBarn(barnRequest, barnId, søknadId, saksbehandlerId, token)
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

private suspend fun RoutingContext.slettBarn(
    opplysningService: OpplysningService,
    søknadId: UUID,
    barnId: UUID,
) {
    val token =
        call.request.headers["Authorization"]?.removePrefix("Bearer ")
            ?: throw IllegalArgumentException("Fant ikke token i request header")

    val slettRequest: SlettBarnRequestDTO
    try {
        slettRequest = call.receive<SlettBarnRequestDTO>()
    } catch (e: Exception) {
        call.respond(
            HttpStatusCode.BadRequest,
            "Kunne ikke parse request body til SlettBarnRequestDTO. Feilmelding: $e",
        )
        return
    }

    if (opplysningService.hentBarn(søknadId).none { it.barnId == barnId }) {
        call.respond(HttpStatusCode.NotFound, "Fant ikke barn med id $barnId for søknad $søknadId")
        return
    }

    val saksbehandlerId = call.saksbehandlerId()
    sikkerlogg.info { "POST /barn slettBarn: saksbehandlerId=$saksbehandlerId, barnId=$barnId" }

    opplysningService.slettBarn(slettRequest, barnId, søknadId, saksbehandlerId, token)
    OpplysningMetrikker.endringBarn.inc()
    call.respond(HttpStatusCode.OK)
}

private suspend fun RoutingContext.oppdaterBarnDeprecated(
    opplysningService: OpplysningService,
    søknadId: UUID,
) {
    val barnRequest: BarnRequestDTO
    val token =
        call.request.headers["Authorization"]?.removePrefix("Bearer ")
            ?: throw IllegalArgumentException("Fant ikke token i request header")

    try {
        barnRequest = call.receive<BarnRequestDTO>()
    } catch (e: Exception) {
        call.respond(
            HttpStatusCode.BadRequest,
            "Kunne ikke parse request body til BarnRequestDTO. Feilmelding: $e",
        )
        return
    }

    // Deprecated endpoint har ikke barnId i path — frontend må migrere til nye PUT
    call.respond(
        HttpStatusCode.BadRequest,
        "Denne endepunktet er deprekert. Bruk PUT /opplysninger/barn/{soknadbarnId}/{barnId} i stedet.",
    )
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
