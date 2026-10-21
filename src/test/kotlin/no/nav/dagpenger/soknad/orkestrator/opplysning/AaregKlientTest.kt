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
import kotlin.test.Test

internal class AaregKlientTest {
    @Test
    fun `returnerer arbeidsforhold fra AAREG`() {
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

            klient.hentArbeidsforhold("12345678910", "token") shouldBe aaregResponse
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
                "ansettelsesdetaljer": [
                  {
                    "type": "Ordinaer",
                    "arbeidstidsordning": {
                      "kode": "ikkeSkift",
                      "beskrivelse": "Ikke skift"
                    }
                  }
                ],
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
                "ansettelsesdetaljer": [
                  {
                    "type": "Forenklet"
                  }
                ],
                "navArbeidsforholdId": 34567,
                "permitteringer": []
              },
              {
                "id": "4",
                "type": {
                  "kode": "ordinaertArbeidsforhold",
                  "beskrivelse": "Ordinært arbeidsforhold"
                },
                "arbeidssted": {
                  "type": "Underenhet",
                  "identer": [
                    {
                      "type": "ORGANISASJONSNUMMER",
                      "ident": "839942907"
                    }
                  ]
                },
                "ansettelsesperiode": {
                  "startdato": "2026-09-01"
                },
                "ansettelsesdetaljer": [
                  {
                    "type": "Ordinaer",
                    "arbeidstidsordning": {
                      "kode": "ikkeSkift",
                      "beskrivelse": "Ikke skift"
                    },
                    "ansettelsesform": {
                      "kode": "fast",
                      "beskrivelse": "Fast ansettelse"
                    },
                    "yrke": {
                      "kode": "5141103",
                      "beskrivelse": "FRISØR"
                    },
                    "antallTimerPrUke": 37.5,
                    "avtaltStillingsprosent": 100.0,
                    "sisteStillingsprosentendring": "2026-10-01",
                    "sisteLoennsendring": "2026-10-01",
                    "rapporteringsmaaneder": {
                      "fra": "2026-09",
                      "til": null
                    }
                  }
                ],
                "permitteringer": [
                  {
                    "id": "1",
                    "type": {
                      "kode": "permittering",
                      "beskrivelse": "Permittering"
                    },
                    "startdato": "2026-10-01",
                    "prosent": 100.0
                  }
                ],
                "navArbeidsforholdId": 3186991
              }
            ]
            """.trimIndent()
    }
}
