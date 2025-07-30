package no.nav.dagpenger.soknad.orkestrator.personalia

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.http.HttpStatusCode.Companion.Unauthorized
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.jwt
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.dagpenger.soknad.orkestrator.api.auth.AuthFactory.tokenX
import no.nav.dagpenger.soknad.orkestrator.utils.TestApplication.testTokenXToken
import no.nav.dagpenger.soknad.orkestrator.utils.TestApplication.withMockAuthServerAndTestApplication
import org.junit.jupiter.api.Test
import java.time.LocalDate.now

class PersonaliaApiTest {
    val personaliaService = mockk<PersonaliaService>(relaxed = true)
    val testModuleFunction: Application.() -> Unit = {
        install(plugin = Authentication) {
            jwt(name = "tokenX") {
                tokenX()
            }
        }
        personaliaApi(personaliaService)
    }

    @Test
    fun `GET personalia returnerer 401 Unauthorized hvis klient ikke er autentisert`() {
        withMockAuthServerAndTestApplication(moduleFunction = testModuleFunction) {
            client.get(urlString = "/personalia").status shouldBe Unauthorized
        }
    }

    @Test
    fun `GET personalia returnerer 200 OK med forventet body hvis klient er autentisert`() {
        coEvery { personaliaService.getPersonalia(fnr = any(), subjectToken = any()) } returns
            PersonaliaDto(
                PersonDto(
                    fornavn = "",
                    mellomnavn = "",
                    etternavn = "",
                    fodselsDato = now(),
                    ident = "",
                    postAdresse = null,
                    folkeregistrertAdresse = null,
                ),
                kontonummer = null,
            )

        withMockAuthServerAndTestApplication(moduleFunction = testModuleFunction) {
            val response =
                client
                    .get(urlString = "/personalia") {
                        header(HttpHeaders.Authorization, "Bearer $testTokenXToken")
                    }

            response.status shouldBe OK
            response.body() as PersonaliaDto shouldNotBe null
            response.headers["Content-Type"] shouldBe "application/json; charset=UTF-8"
        }
    }
}
