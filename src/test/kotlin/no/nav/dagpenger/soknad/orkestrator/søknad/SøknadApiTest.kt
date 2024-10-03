package no.nav.dagpenger.soknad.orkestrator.søknad

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import io.mockk.mockk
import no.nav.dagpenger.soknad.orkestrator.api.models.SporsmalgruppeDTO
import no.nav.dagpenger.soknad.orkestrator.config.apiKonfigurasjon
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import no.nav.dagpenger.soknad.orkestrator.utils.TestApplication
import no.nav.dagpenger.soknad.orkestrator.utils.TestApplication.autentisert
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.UUID
import kotlin.test.Test

class SøknadApiTest {
    val søknadEndepunkt = "/soknad"
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
                shouldNotThrow<Exception> { objectMapper.readValue(respons.bodyAsText(), UUID::class.java) }
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
    fun `Returnerer neste spørsmålgruppe for en gitt søknadId`() {
        withSøknadApi {
            autentisert(
                endepunkt = "$søknadEndepunkt/$søknadId/neste",
                httpMethod = HttpMethod.Get,
            ).let { respons ->
                respons.status shouldBe HttpStatusCode.OK
                shouldNotThrow<Exception> {
                    objectMapper.readValue(
                        respons.bodyAsText(),
                        SporsmalgruppeDTO::class.java,
                    )
                }
            }
        }
    }

    @ParameterizedTest
    @MethodSource("alleSvartyperSomJson")
    fun `Kan besvare spørsmål`(jsonSvar: String) {
        withSøknadApi {
            autentisert(
                endepunkt = "$søknadEndepunkt/$søknadId/svar",
                httpMethod = HttpMethod.Put,
                body = jsonSvar,
            ).let { respons ->
                respons.status shouldBe HttpStatusCode.OK
            }
        }
    }

    @Test
    fun `Kan besvare opplysning når type er definert i lowercase`() {
        val jsonSvarMedLowercaseType =
            """
            {
              "opplysningId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
              "type": "boolean",
              "verdi": true
            }
            """.trimIndent()

        withSøknadApi {
            autentisert(
                endepunkt = "$søknadEndepunkt/$søknadId/svar",
                httpMethod = HttpMethod.Put,
                //language=JSON
                body = jsonSvarMedLowercaseType,
            ).let { respons ->
                respons.status shouldBe HttpStatusCode.OK
            }
        }
    }

    @ParameterizedTest
    @MethodSource("svarMedFeilVerditype")
    fun `Kan ikke besvare en opplysning hvor type og verdi ikke samsvarer`(jsonSvarMedFeilVerditype: String) {
        shouldThrow<IllegalArgumentException> {
            withSøknadApi {
                autentisert(
                    endepunkt = "$søknadEndepunkt/$søknadId/svar",
                    httpMethod = HttpMethod.Put,
                    body = jsonSvarMedFeilVerditype,
                ).let { respons ->
                    respons.status shouldBe HttpStatusCode.OK
                }
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

    companion object {
        //language=JSON
        @JvmStatic
        fun alleSvartyperSomJson() =
            listOf(
                """
                {
                  "opplysningId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                  "type": "BOOLEAN",
                  "verdi": true
                }
                """.trimIndent(),
                """
                {
                  "opplysningId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                  "type": "PERIODE",
                  "verdi": {
                    "fom": "2022-01-01",
                    "tom": "2022-12-31"
                  }
                }
                """.trimIndent(),
                """
                {
                  "opplysningId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                  "type": "LAND",
                  "verdi": "NOR"
                }
                """.trimIndent(),
                """
                {
                  "opplysningId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                  "type": "TEKST",
                  "verdi": "Tekst svar"
                }
                """.trimIndent(),
                """
                {
                  "opplysningId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                  "type": "DATO",
                  "verdi": "2022-12-31"
                }
                """.trimIndent(),
            )

        //language=JSON
        @JvmStatic
        fun svarMedFeilVerditype() =
            listOf(
                """
                {
                  "opplysningId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                  "type": "BOOLEAN",
                  "verdi": "dette er ikke en boolean"
                }
                """.trimIndent(),
                """
                {
                  "opplysningId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                  "type": "PERIODE",
                  "verdi": "2022-12-31"
                }
                """.trimIndent(),
                """
                {
                  "opplysningId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                  "type": "LAND",
                  "verdi": true
                }
                """.trimIndent(),
                """
                {
                  "opplysningId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                  "type": "TEKST",
                  "verdi": {
                    "fom": "2022-01-01",
                    "tom": "2022-12-31"
                  }
                }
                """.trimIndent(),
                """
                {
                  "opplysningId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                  "type": "DATO",
                  "verdi": "dette er ikke en dato"
                }
                """.trimIndent(),
            )
    }
}
