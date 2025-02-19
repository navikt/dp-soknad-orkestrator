package no.nav.dagpenger.soknad.orkestrator.opplysning

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import no.nav.dagpenger.soknad.orkestrator.api.auth.AuthFactory.azureAd
import java.time.LocalDate
import java.util.UUID

internal fun Application.opplysningApi(opplysningService: OpplysningService) {
    install(Authentication) {
        jwt("azureAd") { azureAd() }
    }

    routing {
        get("/") { call.respond(HttpStatusCode.OK) }

        authenticate("azureAd") {
            route("/opplysninger") {
                get {
                    call.respond(HttpStatusCode.OK)
                }
                route("/barn") {
                    get {
                        call.respond(HttpStatusCode.OK, opplysningService.hentBarn())
                    }

                    put {
                        val barn = call.receive<BarnDTO>()
                        call.respond(HttpStatusCode.OK)
                    }
                }
            }
        }
    }
}

data class BarnDTO(
    val barnSvarId: UUID,
    val fornavnOgMellomnavn: String,
    val etternavn: String,
    val fødselsdato: LocalDate,
    val oppholdssted: String,
    val forsørgerBarnet: Boolean,
    val fraRegister: Boolean,
    val girBarnetillegg: Boolean,
    val girBarnetilleggFom: LocalDate? = null,
    val girBarnetilleggTom: LocalDate? = null,
    val begrunnelse: String? = null,
    val endretAv: String? = null,
)
