package no.nav.dagpenger.soknad.orkestrator.søknad.seksjon

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.InternalServerError
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.http.HttpStatusCode.Companion.Unauthorized
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

class SeksjonApiTest {
    val seksjonService: SeksjonService = mockk<SeksjonService>(relaxed = true)
    val testModuleFunction: Application.() -> Unit = {
        install(Authentication) {
            jwt("tokenX") {
                tokenX()
            }
        }
        seksjonApi(seksjonService)
    }

    @Test
    fun `PUT seksjon returnerer 401 Unauthorized hvis klient ikke er autentisert`() {
        withMockAuthServerAndTestApplication(moduleFunction = testModuleFunction) {
            client.put("/seksjon/e857fa6d-b004-4e11-84df-ed7a17801ff7/din-situasjon").status shouldBe Unauthorized
        }
    }

    @Test
    fun `PUT seksjon returnerer 400 Bad Request hvis søknadId ikke er en UUID`() {
        withMockAuthServerAndTestApplication(moduleFunction = testModuleFunction) {
            val response =
                client.put("/seksjon/ikke-en-uuid/din-situasjon") {
                    header(HttpHeaders.Authorization, "Bearer $testTokenXToken")
                }

            response.status shouldBe BadRequest
            response.body() as String shouldContain "Kunne ikke parse søknadId parameter ikke-en-uuid til UUID"
        }
    }

    @Test
    fun `PUT søknad returnerer 200 OK hvis lagring av seksjon er vellykket`() {
        every { seksjonService.lagre(any(), any(), any()) } answers {}

        withMockAuthServerAndTestApplication(moduleFunction = testModuleFunction) {
            val response =
                client.put("/seksjon/e857fa6d-b004-4e11-84df-ed7a17801ff7/din-situasjon") {
                    header(HttpHeaders.Authorization, "Bearer $testTokenXToken")
                }

            response.status shouldBe OK
        }
    }

    @Test
    fun `PUT søknad returnerer 500 Internal Server Error hvis lagring av seksjon feiler`() {
        every { seksjonService.lagre(any(), any(), any()) } throws IllegalStateException()

        withMockAuthServerAndTestApplication(moduleFunction = testModuleFunction) {
            val response =
                client.put("/seksjon/e857fa6d-b004-4e11-84df-ed7a17801ff7/din-situasjon") {
                    header(HttpHeaders.Authorization, "Bearer $testTokenXToken")
                }

            response.status shouldBe InternalServerError
        }
    }

    @Test
    fun `GET seksjon returnerer 401 Unauthorized hvis klient ikke er autentisert`() {
        withMockAuthServerAndTestApplication(moduleFunction = testModuleFunction) {
            client.get("/seksjon/e857fa6d-b004-4e11-84df-ed7a17801ff7/din-situasjon").status shouldBe Unauthorized
        }
    }

    @Test
    fun `GET søknad returnerer 200 OK og forventet repsons hvis kombinasjonen av soknadId og seksjonId eksisterer`() {
        every { seksjonService.hent(any(), any()) } returns "{ seksjonId: \"din-situasjon\" }"

        withMockAuthServerAndTestApplication(moduleFunction = testModuleFunction) {
            val response =
                client.get("/seksjon/e857fa6d-b004-4e11-84df-ed7a17801ff7/din-situasjon") {
                    header(HttpHeaders.Authorization, "Bearer $testTokenXToken")
                }

            response.status shouldBe OK
            response.body() as String shouldBe "{ seksjonId: \"din-situasjon\" }"
        }
    }

    @Test
    fun `GET søknad returnerer 404 Not Found hvis kombinasjonen av soknadId og seksjonId ikke eksisterer`() {
        every { seksjonService.hent(any(), any()) } returns null

        withMockAuthServerAndTestApplication(moduleFunction = testModuleFunction) {
            val response =
                client.get("/seksjon/e857fa6d-b004-4e11-84df-ed7a17801ff7/din-situasjon") {
                    header(HttpHeaders.Authorization, "Bearer $testTokenXToken")
                }

            response.status shouldBe NotFound
        }
    }

    @Test
    fun `GET søknad returnerer 500 Internal Server Error hvis kombinasjonen av soknadId og seksjonId ikke eksisterer`() {
        every { seksjonService.hent(any(), any()) } throws IllegalStateException()

        withMockAuthServerAndTestApplication(moduleFunction = testModuleFunction) {
            val response =
                client.get("/seksjon/e857fa6d-b004-4e11-84df-ed7a17801ff7/din-situasjon") {
                    header(HttpHeaders.Authorization, "Bearer $testTokenXToken")
                }

            response.status shouldBe InternalServerError
        }
    }

    @Test
    fun `GET søknad fremgang returnerer 200 OK og liste av seksjoner lagret`() {
        every { seksjonService.hentLagredeSeksjonerForGittSøknadId(any()) } returns
            listOf(
                "din-situasjon",
                "utdanning",
                "barnetillegg",
            )

        withMockAuthServerAndTestApplication(moduleFunction = testModuleFunction) {
            val response =
                client.get("/soknad/e857fa6d-b004-4e11-84df-ed7a17801ff7/progress") {
                    header(HttpHeaders.Authorization, "Bearer $testTokenXToken")
                }

            response.status shouldBe OK
            response.body() as String shouldBe "{\"seksjoner\":[\"din-situasjon\",\"utdanning\",\"barnetillegg\"]}"
        }
    }

    @Test
    fun `GET søknad fremgang returnerer 404 Not Found hvis det ikke er lagret `() {
        every { seksjonService.hentLagredeSeksjonerForGittSøknadId(any()) } returns
            listOf()

        withMockAuthServerAndTestApplication(moduleFunction = testModuleFunction) {
            val response =
                client.get("/soknad/e857fa6d-b004-4e11-84df-ed7a17801ff7/progress") {
                    header(HttpHeaders.Authorization, "Bearer $testTokenXToken")
                }

            response.status shouldBe NotFound
            response.body() as String shouldBe "{\"seksjoner\":[]}"
        }
    }

    @Test
    fun `GET søknad fremgang returnerer 500 Internal Server Error hvis kall fra repository kaster IllegalStateException`() {
        every { seksjonService.hentLagredeSeksjonerForGittSøknadId(any()) } throws IllegalStateException()
        withMockAuthServerAndTestApplication(moduleFunction = testModuleFunction) {
            val response =
                client.get("/soknad/e857fa6d-b004-4e11-84df-ed7a17801ff7/progress") {
                    header(HttpHeaders.Authorization, "Bearer $testTokenXToken")
                }

            response.status shouldBe InternalServerError
        }
    }

    @Test
    fun `GET progress returnerer 401 Unauthorized hvis klient ikke er autentisert`() {
        withMockAuthServerAndTestApplication(moduleFunction = testModuleFunction) {
            client.get("/soknad/e857fa6d-b004-4e11-84df-ed7a17801ff7/progress").status shouldBe Unauthorized
        }
    }
}
