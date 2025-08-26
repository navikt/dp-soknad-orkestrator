package no.nav.dagpenger.soknad.orkestrator.barn

import io.kotest.inspectors.forExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
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

class BarnApiTest {
    val barnService = mockk<BarnService>(relaxed = true)
    val testModuleFunction: Application.() -> Unit = {
        install(plugin = Authentication) {
            jwt(name = "tokenX") {
                tokenX()
            }
        }
        barnApi(barnService)
    }

    @Test
    fun `GET barn returnerer 401 Unauthorized hvis klient ikke er autentisert`() {
        withMockAuthServerAndTestApplication(moduleFunction = testModuleFunction) {
            client.get(urlString = "/barn").status shouldBe Unauthorized
        }
    }

    @Test
    fun `GET barn returnerer 200 OK med forventet body hvis klient er autentisert`() {
        coEvery { barnService.hentBarn(fnr = any()) } returns
            listOf(
                BarnDto(
                    fornavn = "fornavn1",
                    mellomnavn = "mellomnavn1",
                    etternavn = "etternavn1",
                    fødselsdato = now(),
                    bostedsland = "bostedsland1",
                ),
                BarnDto(
                    fornavn = "fornavn2",
                    mellomnavn = "mellomnavn2",
                    etternavn = "etternavn2",
                    fødselsdato = now(),
                    bostedsland = "bostedsland2",
                ),
            )

        withMockAuthServerAndTestApplication(moduleFunction = testModuleFunction) {
            val response =
                client
                    .get(urlString = "/barn") {
                        header(HttpHeaders.Authorization, "Bearer $testTokenXToken")
                    }

            val barnDtoer = response.body() as List<BarnDto>
            response.status shouldBe OK
            barnDtoer shouldHaveSize 2
            barnDtoer.forExactly(1) { it.fornavnOgMellomnavn shouldBe "fornavn1 mellomnavn1" }
            barnDtoer.forExactly(1) { it.fornavnOgMellomnavn shouldBe "fornavn2 mellomnavn2" }
            response.headers["Content-Type"] shouldBe "application/json; charset=UTF-8"
        }
    }
}
