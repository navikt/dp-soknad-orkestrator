package no.nav.dagpenger.soknad.orkestrator.søknad

import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.Created
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
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
                route("/mine-soknader") {
                    get {
                        val ident = call.ident()
                        val søknader = søknadService.hentSøknaderForIdent(ident)
                        call.respond(OK, søknader)
                    }
                }
            }
            route("/soknad/{søknadId}") {
                post {
                    val søknadId =
                        validerOgFormaterSøknadIdParam() ?: let {
                            call.respond(BadRequest)
                            return@post
                        }

                    call.respondText(
                        status = OK,
                        text = søknadService.sendInn(søknadId, call.ident()).toString(),
                    )
                }
                delete {
                    val søknadId = validerOgFormaterSøknadIdParam() ?: return@delete

                    søknadService.slettSøknadInkrementerMetrikkOgSendMeldingOmSletting(søknadId, call.ident())

                    call.respond(OK, "Søknad $søknadId er slettet")
                }
                route("/progress") {
                    get {
                        val søknadId =
                            validerOgFormaterSøknadIdParam() ?: let {
                                call.respond(BadRequest)
                                return@get
                            }

                        val progress =
                            seksjonService.hentSeksjonIdForAlleLagredeSeksjoner(søknadId, call.ident())

                        if (progress.isEmpty()) {
                            call.respond(NotFound, mapOf("seksjoner" to progress))
                            return@get
                        }

                        call.respond(OK, mapOf("seksjoner" to progress))
                    }
                }
                route("/dokumentasjonskrav") {
                    get {
                        val søknadId =
                            validerOgFormaterSøknadIdParam() ?: let {
                                call.respond(BadRequest)
                                return@get
                            }

                        val dokumentasjonskrav =
                            søknadService.hentDokumentasjonskrav(søknadId, call.ident())

                        if (dokumentasjonskrav.isEmpty()) {
                            call.respond(NotFound)
                            return@get
                        }

                        call.respond(OK, dokumentasjonskrav)
                    }
                }
                route("/personalia") {
                    put {
                        val søknadId =
                            validerOgFormaterSøknadIdParam() ?: let {
                                call.respond(BadRequest)
                                return@put
                            }

                        val putSøknadPersonaliaRequestBody = call.receive<PutSøknadPersonaliaRequestBody>()
                        søknadService.lagrePersonalia(
                            SøknadPersonalia(
                                søknadId,
                                call.ident(),
                                putSøknadPersonaliaRequestBody.fornavn,
                                putSøknadPersonaliaRequestBody.mellomnavn,
                                putSøknadPersonaliaRequestBody.etternavn,
                                putSøknadPersonaliaRequestBody.alder,
                                putSøknadPersonaliaRequestBody.adresselinje1,
                                putSøknadPersonaliaRequestBody.adresselinje2,
                                putSøknadPersonaliaRequestBody.adresselinje3,
                                putSøknadPersonaliaRequestBody.postnummer,
                                putSøknadPersonaliaRequestBody.poststed,
                                putSøknadPersonaliaRequestBody.landkode,
                                putSøknadPersonaliaRequestBody.land,
                                putSøknadPersonaliaRequestBody.kontonummer,
                            ),
                        )

                        call.respond(OK)
                    }
                }
                route("/sistoppdatert") {
                    get {
                        val søknadId =
                            validerOgFormaterSøknadIdParam() ?: let {
                                call.respond(BadRequest)
                                return@get
                            }

                        val sistOppdatert = søknadService.hentSistOppdatertTidspunkt(søknadId)

                        if (sistOppdatert == null) {
                            call.respond(NotFound, "Fant ikke oppdatert-tidspunkt for søknad $søknadId")
                            return@get
                        }

                        call.respond(OK, sistOppdatert)
                    }
                }
            }
        }
    }
}
