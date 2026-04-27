package no.nav.dagpenger.soknad.orkestrator.saf

import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.oauth2.CachedOauth2Client
import no.nav.dagpenger.soknad.orkestrator.utils.configureHttpClient
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenResponse
import kotlin.test.Test

class SafKlientTest {
    private val azureAdClient = mockk<CachedOauth2Client>()
    private val tokenResponse = mockk<OAuth2AccessTokenResponse>()

    init {
        every { tokenResponse.access_token } returns "test-token"
        every { azureAdClient.clientCredentials(any()) } returns tokenResponse
    }

    private fun lagSafKlient(mockEngine: MockEngine) =
        SafKlient(
            azureAdClient = azureAdClient,
            safUrl = "http://localhost",
            safScope = "test-scope",
            httpClient = configureHttpClient(mockEngine),
        )

    @Test
    fun `hentSøknadUuid returnerer null for NAVe-brevkode uten å hente dokumentinnhold`() {
        var antallRequests = 0
        val mockEngine =
            MockEngine { _ ->
                antallRequests++
                respond(
                    content = ettersendingGraphQlRespons,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                )
            }

        val resultat = lagSafKlient(mockEngine).hentSøknadUuid("123456789")

        resultat shouldBe null
        antallRequests shouldBe 1
    }

    @Test
    fun `hentSøknadUuid henter dokumentinnhold for ikke-NAVe-brevkode`() {
        var antallRequests = 0
        val mockEngine =
            MockEngine { _ ->
                antallRequests++
                if (antallRequests == 1) {
                    respond(
                        content = vanligSøknadGraphQlRespons,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                } else {
                    respond(
                        content = """{"søknad_uuid": "550e8400-e29b-41d4-a716-446655440000"}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }
            }

        val resultat = lagSafKlient(mockEngine).hentSøknadUuid("123456789")

        resultat.toString() shouldBe "550e8400-e29b-41d4-a716-446655440000"
        antallRequests shouldBe 2
    }
}

private val ettersendingGraphQlRespons =
    """
    {
      "data": {
        "journalpost": {
          "journalpostId": "123456789",
          "dokumenter": [
            {
              "dokumentInfoId": "dok-1",
              "brevkode": "NAVe 04-01.03",
              "tittel": "Ettersending til søknad om dagpenger"
            }
          ]
        }
      }
    }
    """.trimIndent()

private val vanligSøknadGraphQlRespons =
    """
    {
      "data": {
        "journalpost": {
          "journalpostId": "123456789",
          "dokumenter": [
            {
              "dokumentInfoId": "dok-1",
              "brevkode": "NAV 04-01.03",
              "tittel": "Søknad om dagpenger"
            }
          ]
        }
      }
    }
    """.trimIndent()
