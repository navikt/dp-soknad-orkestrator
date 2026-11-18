package no.nav.dagpenger.soknad.orkestrator.behov.api

import io.kotest.matchers.shouldBe
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.jwt
import no.nav.dagpenger.soknad.orkestrator.api.auth.AuthFactory.azureAdMaskin
import no.nav.dagpenger.soknad.orkestrator.utils.TestApplication.testAzureADMaskinToken
import no.nav.dagpenger.soknad.orkestrator.utils.TestApplication.testAzureADToken
import no.nav.dagpenger.soknad.orkestrator.utils.TestApplication.withMockAuthServerAndTestApplication
import org.junit.jupiter.api.Test

class BehovApiTest {
    private val testModul: Application.() -> Unit = {
        install(Authentication) {
            jwt("azureAd-maskin") { azureAdMaskin() }
        }
        behovApi()
    }

    @Test
    fun `godtar app-token`() =
        withMockAuthServerAndTestApplication(testModul) {
            client.post("/behov") { bearerAuth(testAzureADMaskinToken()) }.status shouldBe HttpStatusCode.OK
        }

    @Test
    fun `avviser token uten idtyp app`() =
        withMockAuthServerAndTestApplication(testModul) {
            client
                .post("/behov") { bearerAuth(testAzureADMaskinToken(claims = emptyMap())) }
                .status shouldBe HttpStatusCode.Unauthorized
        }

    @Test
    fun `avviser saksbehandlertoken`() =
        withMockAuthServerAndTestApplication(testModul) {
            client.post("/behov") { bearerAuth(testAzureADToken) }.status shouldBe HttpStatusCode.Unauthorized
        }

    @Test
    fun `avviser kall uten token`() =
        withMockAuthServerAndTestApplication(testModul) {
            client.post("/behov").status shouldBe HttpStatusCode.Unauthorized
        }
}
