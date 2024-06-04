package no.nav.dagpenger.soknad.orkestrator.søknad

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.matchers.shouldBe
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import io.mockk.mockk
import no.nav.dagpenger.soknad.orkestrator.config.apiKonfigurasjon
import no.nav.dagpenger.soknad.orkestrator.spørsmål.SpørsmålDTO
import no.nav.dagpenger.soknad.orkestrator.spørsmål.SpørsmålType
import no.nav.dagpenger.soknad.orkestrator.spørsmål.grupper.SpørsmålgruppeDTO
import no.nav.dagpenger.soknad.orkestrator.utils.TestApplication
import no.nav.dagpenger.soknad.orkestrator.utils.TestApplication.autentisert
import org.junit.jupiter.api.Disabled
import java.util.UUID
import kotlin.test.Test

class SøknadApiTest {
    val søknadEndepunkt = "/soknad"
    val jacksonMapper = jacksonObjectMapper()
    val søknadId = UUID.randomUUID()
    val ident = "12345678901"

    val søknadService = mockk<SøknadService>(relaxed = true)

    @Test
    fun `Start-søknad svarer med en uuid`() {
        withSøknadApi {
            autentisert(
                endepunkt = "$søknadEndepunkt/start",
                httpMethod = HttpMethod.Post,
            ).let { respons ->
                respons.status shouldBe HttpStatusCode.Created
                shouldNotThrow<Exception> { jacksonMapper.readValue(respons.bodyAsText(), UUID::class.java) }
            }
        }
    }

    @Test
    fun `Uautentiserte kall responderer med Unauthorized`() {
        withSøknadApi {
            client.post("$søknadEndepunkt/start").status shouldBe HttpStatusCode.Unauthorized
        }
    }

    @Test
    fun `Returnerer neste spørsmål for en gitt søknadId`() {
        withSøknadApi {
            autentisert(
                endepunkt = "$søknadEndepunkt/$søknadId/neste",
                httpMethod = HttpMethod.Get,
            ).let { respons ->
                respons.status shouldBe HttpStatusCode.OK
                shouldNotThrow<Exception> { jacksonMapper.readValue(respons.bodyAsText(), SpørsmålgruppeDTO::class.java) }
            }
        }
    }

    @Disabled
    @Test
    fun `Returnerer NoContent hvis ingen nye spørsmål for gitt søknadId`() {
        withSøknadApi {
            autentisert(
                endepunkt = "$søknadEndepunkt/$søknadId/neste",
                httpMethod = HttpMethod.Get,
            ).let { respons ->
                respons.status shouldBe HttpStatusCode.NoContent
            }
        }
    }

    @Test
    fun `Kan besvare spørsmål`() {
        withSøknadApi {
            autentisert(
                endepunkt = "$søknadEndepunkt/$søknadId/svar",
                httpMethod = HttpMethod.Post,
                body =
                    jacksonMapper.writeValueAsString(
                        SpørsmålDTO(
                            id = UUID.randomUUID(),
                            tekstnøkkel = "tekstnøkkel.test",
                            type = SpørsmålType.BOOLEAN,
                            svar = true,
                        ),
                    ),
            ).let { respons ->
                respons.status shouldBe HttpStatusCode.OK
            }
        }
    }

    private fun withSøknadApi(test: suspend ApplicationTestBuilder.() -> Unit) {
        TestApplication.withMockAuthServerAndTestApplication(
            moduleFunction = {
                apiKonfigurasjon()
                søknadApi(søknadService)
            },
            test = test,
        )
    }
}
