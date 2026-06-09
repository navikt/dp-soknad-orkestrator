package no.nav.dagpenger.soknad.orkestrator.utils

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson3.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.testing.testApplication
import no.nav.dagpenger.soknad.orkestrator.config.configure
import no.nav.security.mock.oauth2.MockOAuth2Server

class TestContext(
    val client: HttpClient,
)

object TestApplication {
    private const val TOKENX_ISSUER_ID = "tokenx"
    private const val AZUREAD_ISSUER_ID = "azureAd"
    private const val CLIENT_ID = "dp-soknad-orkestrator"
    const val DEFAULT_DUMMY_FODSELSNUMMER = "12345678910"

    private val mockOAuth2Server: MockOAuth2Server by lazy {
        MockOAuth2Server().also { server ->
            server.start()
        }
    }

    internal val testTokenXToken: String by lazy {
        mockOAuth2Server
            .issueToken(
                issuerId = TOKENX_ISSUER_ID,
                audience = CLIENT_ID,
                claims = mapOf("pid" to DEFAULT_DUMMY_FODSELSNUMMER),
            ).serialize()
    }

    internal val testAzureADToken: String by lazy {
        mockOAuth2Server
            .issueToken(
                issuerId = AZUREAD_ISSUER_ID,
                audience = CLIENT_ID,
                claims =
                    mapOf(
                        "NAVident" to "123",
                        "groups" to listOf("saksbehandler"),
                    ),
            ).serialize()
    }

    internal fun withMockAuthServerAndTestApplication(
        moduleFunction: Application.() -> Unit,
        test: suspend TestContext.() -> Unit,
    ) {
        System.setProperty("token-x.client-id", CLIENT_ID)
        System.setProperty("token-x.well-known-url", "${mockOAuth2Server.wellKnownUrl(TOKENX_ISSUER_ID)}")
        System.setProperty("azure-app.client-id", CLIENT_ID)
        System.setProperty("azure-app.well-known-url", "${mockOAuth2Server.wellKnownUrl(AZUREAD_ISSUER_ID)}")

        testApplication {
            application {
                install(io.ktor.server.plugins.contentnegotiation.ContentNegotiation) {
                    jackson { configure() }
                }

                moduleFunction()
            }
            val client =
                createClient {
                    install(ContentNegotiation) {
                        jackson { configure() }
                    }
                }
            TestContext(client).test()
        }
    }
}
