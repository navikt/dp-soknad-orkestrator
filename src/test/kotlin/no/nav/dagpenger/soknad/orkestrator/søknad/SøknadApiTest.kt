package no.nav.dagpenger.soknad.orkestrator.søknad

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.Created
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
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonService
import no.nav.dagpenger.soknad.orkestrator.utils.TestApplication.testTokenXToken
import no.nav.dagpenger.soknad.orkestrator.utils.TestApplication.withMockAuthServerAndTestApplication
import org.junit.jupiter.api.Test
import java.util.UUID

class SøknadApiTest {
    val søknadService = mockk<SøknadService>(relaxed = true)
    val seksjonService = mockk<SeksjonService>(relaxed = true)
    val testModuleFunction: Application.() -> Unit = {
        install(Authentication) {
            jwt("tokenX") {
                tokenX()
            }
        }
        søknadApi(søknadService, seksjonService)
    }

    @Test
    fun `POST søknad returnerer 401 Unauthorized hvis klient ikke er autentisert`() {
        withMockAuthServerAndTestApplication(moduleFunction = testModuleFunction) {
            client.post("/soknad").status shouldBe Unauthorized
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

            response.status shouldBe Created
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

            response.status shouldBe InternalServerError
        }
    }

    @Test
    fun `POST søknad med søknadId returnerer 401 Unauthorized hvis klient ikke er autentisert`() {
        withMockAuthServerAndTestApplication(moduleFunction = testModuleFunction) {
            client.post("/soknad/${UUID.randomUUID()}").status shouldBe Unauthorized
        }
    }

    @Test
    fun `POST søknad med søknadId returnerer 200 OK hvis markering av søknaden som innsendt går bra`() {
        withMockAuthServerAndTestApplication(moduleFunction = testModuleFunction) {
            val response =
                client
                    .post("/soknad/${UUID.randomUUID()}") {
                        header(HttpHeaders.Authorization, "Bearer $testTokenXToken")
                    }

            response.status shouldBe OK
        }
    }

    @Test
    fun `POST søknad med søknadId returnerer 500 Internal Server Error hvis markering av søknaden som innsendt feiler`() {
        every { søknadService.sendInn(any(), any()) } throws IllegalStateException()

        withMockAuthServerAndTestApplication(moduleFunction = testModuleFunction) {
            val response =
                client
                    .post("/soknad/${UUID.randomUUID()}") {
                        header(HttpHeaders.Authorization, "Bearer $testTokenXToken")
                    }

            response.status shouldBe InternalServerError
        }
    }

    @Test
    fun `GET søknad progress returnerer 200 OK og liste av seksjoner lagret`() {
        every { seksjonService.hentSeksjonIdForAlleLagredeSeksjoner(any(), any()) } returns
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
    fun `GET søknad progress returnerer 404 Not Found ingen seksjoner er lagret`() {
        every { seksjonService.hentSeksjonIdForAlleLagredeSeksjoner(any(), any()) } returns
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
    fun `GET søknad progress returnerer 500 Internal Server Error hvis kall fra repository kaster IllegalStateException`() {
        every { seksjonService.hentSeksjonIdForAlleLagredeSeksjoner(any(), any()) } throws IllegalStateException()
        withMockAuthServerAndTestApplication(moduleFunction = testModuleFunction) {
            val response =
                client.get("/soknad/e857fa6d-b004-4e11-84df-ed7a17801ff7/progress") {
                    header(HttpHeaders.Authorization, "Bearer $testTokenXToken")
                }

            response.status shouldBe InternalServerError
        }
    }

    @Test
    fun `GET søknad progress returnerer 401 Unauthorized hvis klient ikke er autentisert`() {
        withMockAuthServerAndTestApplication(moduleFunction = testModuleFunction) {
            client.get("/soknad/e857fa6d-b004-4e11-84df-ed7a17801ff7/progress").status shouldBe Unauthorized
        }
    }

    @Test
    fun `GET søknad progress returnerer 400 Bad Request hvis søknadId ikke er en UUID`() {
        withMockAuthServerAndTestApplication(moduleFunction = testModuleFunction) {
            val response =
                client.get("/soknad/ikke-en-uuid/progress") {
                    header(HttpHeaders.Authorization, "Bearer $testTokenXToken")
                }

            response.status shouldBe BadRequest
            response.body() as String shouldContain "Kunne ikke parse søknadId parameter ikke-en-uuid til UUID"
        }
    }

    @Test
    fun `DELETE søknad med søknadId returnerer 200 OK hvis sletting av seksjoner går bra`() {
        val søknadId = UUID.randomUUID()
        every {
            søknadService.slett(søknadId, any())
        } returns Unit
        withMockAuthServerAndTestApplication(moduleFunction = testModuleFunction) {
            val response =
                client.delete("/soknad/$søknadId") {
                    header(HttpHeaders.Authorization, "Bearer $testTokenXToken")
                }

            response.status shouldBe OK
        }
    }

    @Test
    fun `DELETE søknad med søknadId returnerer 401 Unauthorized hvis klient ikke er autentisert`() {
        val søknadId = UUID.randomUUID()
        withMockAuthServerAndTestApplication(moduleFunction = testModuleFunction) {
            val response =
                client.delete("/soknad/$søknadId")

            response.status shouldBe Unauthorized
        }
    }

    @Test
    fun `DELETE søknad med søknadId returnerer 400 Bad Request hvis søknadId ikke er en UUID`() {
        withMockAuthServerAndTestApplication(moduleFunction = testModuleFunction) {
            val response =
                client.delete("/soknad/ikke-en-uuid") {
                    header(HttpHeaders.Authorization, "Bearer $testTokenXToken")
                }

            response.status shouldBe BadRequest
            response.body() as String shouldContain "Kunne ikke parse søknadId parameter ikke-en-uuid til UUID"
        }
    }

    @Test
    fun `DELETE søknad med søknadId returnerer 500 Internal Server Error hvis sletting av seksjoner feiler`() {
        val søknadId = UUID.randomUUID()
        every {
            søknadService.slett(søknadId, any())
        } throws IllegalStateException()
        withMockAuthServerAndTestApplication(moduleFunction = testModuleFunction) {
            val response =
                client.delete("/soknad/$søknadId") {
                    header(HttpHeaders.Authorization, "Bearer $testTokenXToken")
                }

            response.status shouldBe InternalServerError
        }
    }
}
