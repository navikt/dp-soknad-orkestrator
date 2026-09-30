package no.nav.dagpenger.soknad.orkestrator.opplysning

import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.soknad.orkestrator.utils.configureHttpClient
import java.time.LocalDate
import kotlin.test.Test

internal class AaregKlientTest {
    @Test
    fun `mapper arbeidsforhold fra AAREG-respons`() {
        runBlocking {
            val klient =
                AaregKlient(
                    aaregUrl = "http://localhost",
                    tokenProvider = { "token" },
                    httpKlient =
                        configureHttpClient(
                            MockEngine {
                                respond(
                                    content = aaregResponse,
                                    status = HttpStatusCode.OK,
                                    headers =
                                        headersOf(
                                            HttpHeaders.ContentType,
                                            ContentType.Application.Json.toString(),
                                        ),
                                )
                            },
                        ),
                )

            klient.hentArbeidsforhold("12345678910", "token") shouldBe
                listOf(
                    Arbeidsforhold(
                        id = "12345",
                        organisasjonsnummer = "910825518",
                        startdato = LocalDate.parse("2014-01-01"),
                        sluttdato = null,
                    ),
                    Arbeidsforhold(
                        id = "34567",
                        organisasjonsnummer = null,
                        startdato = LocalDate.parse("2020-01-01"),
                        sluttdato = LocalDate.parse("2020-01-03"),
                    ),
                )
        }
    }

    private companion object {
        private val aaregResponse =
            """
            [
              {
                "id": "V911050676R16054L0001",
                "arbeidssted": {
                  "type": "Underenhet",
                  "identer": [
                    {
                      "type": "ORGANISASJONSNUMMER",
                      "ident": "910825518"
                    }
                  ]
                },
                "ansettelsesperiode": {
                  "startdato": "2014-01-01"
                },
                "navArbeidsforholdId": 12345
              },
              {
                "id": null,
                "arbeidssted": {
                  "type": "Person",
                  "identer": [
                    {
                      "type": "FOLKEREGISTERIDENT",
                      "ident": "20895298795"
                    }
                  ]
                },
                "ansettelsesperiode": {
                  "startdato": "2020-01-01",
                  "sluttdato": "2020-01-03"
                },
                "navArbeidsforholdId": 34567
              }
            ]
            """.trimIndent()
    }
}
