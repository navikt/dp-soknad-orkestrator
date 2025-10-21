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
import kotlin.test.Test

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
        every { seksjonService.lagre(any(), any(), any(), any()) } answers {}

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
        every { seksjonService.lagre(any(), any(), any(), any()) } throws IllegalStateException()

        withMockAuthServerAndTestApplication(moduleFunction = testModuleFunction) {
            val response =
                client.put("/seksjon/e857fa6d-b004-4e11-84df-ed7a17801ff7/din-situasjon") {
                    header(HttpHeaders.Authorization, "Bearer $testTokenXToken")
                }

            response.status shouldBe InternalServerError
        }
    }

    @Test
    fun `GET gitt søknadId og seksjonId returnerer 401 Unauthorized hvis klient ikke er autentisert`() {
        withMockAuthServerAndTestApplication(moduleFunction = testModuleFunction) {
            client.get("/seksjon/e857fa6d-b004-4e11-84df-ed7a17801ff7/din-situasjon").status shouldBe Unauthorized
        }
    }

    @Test
    fun `GET gitt søknadId og seksjonId returnerer 200 OK og forventet repsons hvis kombinasjonen av soknadId og seksjonId eksisterer`() {
        every { seksjonService.hent(any(), any(), any()) } returns "{ seksjonId: \"din-situasjon\" }"

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
    fun `GET gitt søknadId og seksjonId returnerer 404 Not Found hvis kombinasjonen av soknadId og seksjonId ikke eksisterer`() {
        every { seksjonService.hent(any(), any(), any()) } returns null

        withMockAuthServerAndTestApplication(moduleFunction = testModuleFunction) {
            val response =
                client.get("/seksjon/e857fa6d-b004-4e11-84df-ed7a17801ff7/din-situasjon") {
                    header(HttpHeaders.Authorization, "Bearer $testTokenXToken")
                }

            response.status shouldBe NotFound
        }
    }

    @Test
    @Suppress("ktlint:standard:max-line-length")
    fun `GET gitt søknadId og seksjonId returnerer 500 Internal Server Error dersom uthenting av seksjoner feiler`() {
        every { seksjonService.hent(any(), any(), any()) } throws IllegalStateException()

        withMockAuthServerAndTestApplication(moduleFunction = testModuleFunction) {
            val response =
                client.get("/seksjon/e857fa6d-b004-4e11-84df-ed7a17801ff7/din-situasjon") {
                    header(HttpHeaders.Authorization, "Bearer $testTokenXToken")
                }

            response.status shouldBe InternalServerError
        }
    }

    @Test
    fun `GET gitt søknadId og seksjonId returnerer 400 Bad Request hvis søknadId ikke er en UUID`() {
        withMockAuthServerAndTestApplication(moduleFunction = testModuleFunction) {
            val response =
                client.get("/seksjon/ikke-en-uuid/din-situasjon") {
                    header(HttpHeaders.Authorization, "Bearer $testTokenXToken")
                }

            response.status shouldBe BadRequest
            response.body() as String shouldContain "Kunne ikke parse søknadId parameter ikke-en-uuid til UUID"
        }
    }

    @Test
    fun `GET alle seksjoner gitt søknadId returnerer 200 OK og forventet respons hvis søknadId har seksjoner`() {
        every { seksjonService.hentAlle(any(), any()) } returns
            listOf(
                Seksjon("seksjon-id", "{\"key\": \"value\"}"),
                Seksjon("seksjon-id-2", "{\"key2\": \"value2\"}"),
            )

        withMockAuthServerAndTestApplication(moduleFunction = testModuleFunction) {
            val response =
                client.get("/seksjon/e857fa6d-b004-4e11-84df-ed7a17801ff7") {
                    header(HttpHeaders.Authorization, "Bearer $testTokenXToken")
                }

            response.status shouldBe OK
            with(response.body() as String) {
                this shouldContain """{"seksjonId":"seksjon-id","data":"{\"key\": \"value\"}"}"""
                this shouldContain """{"seksjonId":"seksjon-id-2","data":"{\"key2\": \"value2\"}"}"""
            }
        }
    }

    @Test
    fun `GET alle seksjoner gitt søknadId returnerer 404 Not Found hvis soknadId ikke har noen seksjoner`() {
        every { seksjonService.hentAlle(any(), any()) } returns emptyList()

        withMockAuthServerAndTestApplication(moduleFunction = testModuleFunction) {
            val response =
                client.get("/seksjon/e857fa6d-b004-4e11-84df-ed7a17801ff7") {
                    header(HttpHeaders.Authorization, "Bearer $testTokenXToken")
                }

            response.status shouldBe NotFound
        }
    }

    @Test
    fun `GET alle seksjoner gitt søknadId returnerer 500 Internal Server Error hvis uthenting av seksjoner feiler`() {
        every { seksjonService.hentAlle(any(), any()) } throws IllegalStateException()

        withMockAuthServerAndTestApplication(moduleFunction = testModuleFunction) {
            val response =
                client.get("/seksjon/e857fa6d-b004-4e11-84df-ed7a17801ff7") {
                    header(HttpHeaders.Authorization, "Bearer $testTokenXToken")
                }

            response.status shouldBe InternalServerError
        }
    }

    @Test
    fun `GET alle seksjoner gitt søknadId returnerer 400 Bad Request hvis søknadId ikke er en UUID`() {
        withMockAuthServerAndTestApplication(moduleFunction = testModuleFunction) {
            val response =
                client.get("/seksjon/ikke-en-uuid") {
                    header(HttpHeaders.Authorization, "Bearer $testTokenXToken")
                }

            response.status shouldBe BadRequest
            response.body() as String shouldContain "Kunne ikke parse søknadId parameter ikke-en-uuid til UUID"
        }
    }
}
