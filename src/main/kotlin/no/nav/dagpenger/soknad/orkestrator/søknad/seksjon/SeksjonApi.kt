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
            route("/seksjon/{søknadId}/{seksjonId}") {
                put {
                    val søknadId = validerOgFormaterSøknadIdParam() ?: return@put
                    val seksjonId = validerSeksjonIdParam() ?: return@put
                    val putSeksjonRequestBody = call.receive<PutSeksjonRequestBody>()
                    seksjonService.lagre(
                        call.ident(),
                        søknadId,
                        seksjonId,
                        putSeksjonRequestBody.seksjonsvar,
                        putSeksjonRequestBody.dokumentasjonskrav,
                        putSeksjonRequestBody.pdfGrunnlag,
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

data class PutSeksjonRequestBody(
    val seksjonsvar: String,
    val dokumentasjonskrav: String? = null,
    val pdfGrunnlag: String,
)
