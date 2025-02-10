package no.nav.dagpenger.soknad.orkestrator.opplysning

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.jwt
import no.nav.dagpenger.soknad.orkestrator.api.auth.AuthFactory.azureAd

internal fun Application.opplysningApi() {
    install(Authentication) {
        jwt("azureAd") { azureAd() }
    }
}
