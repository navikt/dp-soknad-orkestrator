package no.nav.dagpenger.soknad.orkestrator.søknad.seksjon

import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import no.nav.dagpenger.soknad.orkestrator.api.auth.ident
import no.nav.dagpenger.soknad.orkestrator.utils.validerOgFormaterSøknadIdParam
import no.nav.dagpenger.soknad.orkestrator.utils.validerSeksjonIdParam

internal fun Application.seksjonApi(seksjonService: SeksjonService) {
    routing {
        authenticate("tokenX") {
            // TODO: Versjonert så vi ikke skal brekke hele frontend før den er skrevet om. Siden vi ikke er i prod enda,
            // TODO: så kan vi vurdere å fjerne versjonsnummeret igjen når frontend bruker V2 til lagring av seksjon.
            route("/seksjon/v2/{søknadId}/{seksjonId}") {
                put {
                    val søknadId = validerOgFormaterSøknadIdParam() ?: return@put
                    val seksjonId = validerSeksjonIdParam() ?: return@put
                    val putSeksjonRequest = call.receive<PutSeksjonRequest>()
                    seksjonService.lagre(
                        call.ident(),
                        søknadId,
                        seksjonId,
                        putSeksjonRequest.seksjonsvar,
                        putSeksjonRequest.dokumentasjonskrav,
                        putSeksjonRequest.pdfGrunnlag,
                    )
                    call.respond(OK)
                }
            }
            route("/seksjon/{søknadId}/{seksjonId}") {
                put {
                    val søknadId = validerOgFormaterSøknadIdParam() ?: return@put
                    val seksjonId = validerSeksjonIdParam() ?: return@put
                    seksjonService.lagre(
                        ident = call.ident(),
                        søknadId = søknadId,
                        seksjonId = seksjonId,
                        seksjonsvar = call.receive<String>(),
                        pdfGrunnlag = "{}",
                    )
                    call.respond(OK)
                }

                get {
                    val søknadId = validerOgFormaterSøknadIdParam() ?: return@get
                    val seksjonId = validerSeksjonIdParam() ?: return@get

                    val seksjon =
                        seksjonService.hentSeksjonsvar(call.ident(), søknadId, seksjonId)
                            ?: run {
                                call.respond(NotFound, "Fant ikke seksjon med id $seksjonId for søknad $søknadId")
                                return@get
                            }

                    call.respond(OK, seksjon)
                }

                route("/dokumentasjonskrav") {
                    put {
                        val søknadId = validerOgFormaterSøknadIdParam() ?: return@put
                        val seksjonId = validerSeksjonIdParam() ?: return@put
                        seksjonService.lagreDokumentasjonskrav(
                            call.ident(),
                            søknadId,
                            seksjonId,
                            call.receive<String>(),
                        )
                        call.respond(OK)
                    }
                }
            }
            route("/seksjon/{søknadId}") {
                get {
                    val søknadId = validerOgFormaterSøknadIdParam() ?: return@get

                    val seksjoner = seksjonService.hentAlleSeksjonsvar(call.ident(), søknadId)

                    if (seksjoner.isEmpty()) {
                        call.respond(NotFound, "Fant ingen seksjoner for søknad $søknadId")
                        return@get
                    }

                    call.respond(OK, seksjoner)
                }
            }
        }
    }
}

data class PutSeksjonRequest(
    val seksjonsvar: String,
    val dokumentasjonskrav: String? = null,
    val pdfGrunnlag: String,
)
