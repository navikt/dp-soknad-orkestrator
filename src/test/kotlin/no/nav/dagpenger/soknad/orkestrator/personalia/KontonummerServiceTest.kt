package no.nav.dagpenger.soknad.orkestrator.personalia

import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.soknad.orkestrator.personalia.KontonummerDto.Companion.TOM
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

internal class KontonummerServiceTest {
    @Test
    fun `hentKontonummer returnerer forventet respons hvis kontoNumberClient returnerer 404 Not Found`() {
        val kontonummerService =
            KontonummerService(
                kontoRegisterUrl = "http://localhost",
                tokenProvider = { "token" },
                httpClientEngine =
                    MockEngine {
                        respondError(NotFound)
                    },
            )
        runBlocking {
            kontonummerService.hentKontonummer("") shouldBe TOM
        }
    }

    @Test
    fun `hentKontonummer returnerer foreventet respons hvis kontoNumberClient returnerer norsk kontonummer`() {
        val kontonummerService =
            KontonummerService(
                kontoRegisterUrl = "http://localhost",
                tokenProvider = { subjektToken -> "token $subjektToken" },
                httpClientEngine =
                    MockEngine { request ->
                        respond(
                            content = norskKontoResponse,
                            status = OK,
                            headers =
                                headersOf(
                                    name = HttpHeaders.ContentType,
                                    value = ContentType.Application.Json.toString(),
                                ),
                        )
                    },
            )
        runBlocking {
            kontonummerService.hentKontonummer(subjectToken = "norskKonto") shouldBe KontonummerDto(kontonummer = "123")
        }
    }

    @Test
    fun `hentKontonummer returnerer foreventet respons hvis kontoNumberClient returnerer utenlandsk kontonummer`() {
        val service =
            KontonummerService(
                kontoRegisterUrl = "http://localhost",
                tokenProvider = { subjektToken -> "token $subjektToken" },
                httpClientEngine =
                    MockEngine { request ->
                        respond(
                            content = utenlandskKontoResponse,
                            status = OK,
                            headers =
                                headersOf(
                                    name = HttpHeaders.ContentType,
                                    value = ContentType.Application.Json.toString(),
                                ),
                        )
                    },
            )
        runBlocking {
            service.hentKontonummer(subjectToken = "utenlandsKkonto") shouldBe
                KontonummerDto(
                    kontonummer = "456",
                    banknavn = "banknavn",
                    bankLandkode = "SE",
                )
        }
    }

    @Language("JSON")
    private val utenlandskKontoResponse =
        """
        {
          "utenlandskKontoInfo": {
            "banknavn": "banknavn",
            "bankkode": "456",
            "bankLandkode": "SE",
            "valutakode": "SEK",
            "swiftBicKode": "SHEDNO22",
            "bankadresse1": "string",
            "bankadresse2": "string",
            "bankadresse3": "string"
          }
        }
        """.trimIndent()

    @Language("JSON")
    private val norskKontoResponse =
        """
        {
          "kontonummer": "123"
        }
        """.trimIndent()
}
