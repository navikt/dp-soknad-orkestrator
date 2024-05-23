package no.nav.dagpenger.soknad.orkestrator.søknad

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.matchers.shouldBe
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.dagpenger.soknad.orkestrator.config.apiKonfigurasjon
import no.nav.dagpenger.soknad.orkestrator.utils.TestApplication
import no.nav.dagpenger.soknad.orkestrator.utils.TestApplication.autentisert
import java.util.UUID
import kotlin.test.Test

class SøknadApiTest {
    val endepunkt = "/start-soknad"
    val jacksonMapper = jacksonObjectMapper()

    @Test
    fun `Start-søknad svarer med en uuid`() {
        withSøknadApi {
            autentisert(
                endepunkt = endepunkt,
                httpMethod = HttpMethod.Post,
            ).let { respons ->
                respons.status shouldBe HttpStatusCode.OK
                shouldNotThrow<Exception> { jacksonMapper.readValue(respons.bodyAsText(), UUID::class.java) }
            }
        }
    }

    @Test
    fun `Uautentiserte kall responderer med Unauthorized`() {
        withSøknadApi {
            client.post(endepunkt).status shouldBe HttpStatusCode.Unauthorized
        }
    }
}

private fun withSøknadApi(test: suspend ApplicationTestBuilder.() -> Unit) {
    TestApplication.withMockAuthServerAndTestApplication(
        moduleFunction = {
            apiKonfigurasjon()
            søknadApi()
        },
        test = test,
    )
}
