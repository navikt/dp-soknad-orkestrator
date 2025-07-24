package no.nav.dagpenger.soknad.orkestrator.søknad

import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.jwt
import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.soknad.orkestrator.api.auth.AuthFactory.tokenX
import no.nav.dagpenger.soknad.orkestrator.utils.TestApplication.testTokenXToken
import no.nav.dagpenger.soknad.orkestrator.utils.TestApplication.withMockAuthServerAndTestApplication
import org.junit.jupiter.api.Test
import java.util.UUID

class SøknadApiTest {
    val søknadService = mockk<SøknadService>(relaxed = true)
    val testModuleFunction: Application.() -> Unit = {
        install(Authentication) {
            jwt("tokenX") {
                tokenX()
            }
        }
        søknadApi(søknadService)
    }

    @Test
    fun `POST søknad returnerer 401 Unauthorized hvis klient ikke er autentisert`() {
        withMockAuthServerAndTestApplication(moduleFunction = testModuleFunction) {
            client.post("/soknad").status shouldBe HttpStatusCode.Unauthorized
        }
    }

    @Test
    fun `POST søknad returnerer 201 Created og søknadId hvis oppretting av søknad er vellykket`() {
        val søknadId = UUID.randomUUID()
        every { søknadService.opprett(any()) } returns søknadId

        withMockAuthServerAndTestApplication(moduleFunction = testModuleFunction) {
            val response =
                client
                    .post("/soknad") {
                        header(HttpHeaders.Authorization, "Bearer $testTokenXToken")
                    }

            response.status shouldBe HttpStatusCode.Created
            response.body() as String shouldBe søknadId.toString()
        }
    }

    @Test
    fun `POST søknad returnerer 500 Internal Server Error hvis oppretting av søknad feiler`() {
        every { søknadService.opprett(any()) } throws IllegalStateException()

        withMockAuthServerAndTestApplication(moduleFunction = testModuleFunction) {
            val response =
                client
                    .post("/soknad") {
                        header(HttpHeaders.Authorization, "Bearer $testTokenXToken")
                    }

            response.status shouldBe HttpStatusCode.InternalServerError
        }
    }
}
