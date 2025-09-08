package no.nav.dagpenger.soknad.orkestrator.utils

import com.github.navikt.tbd_libs.naisful.test.TestContext
import com.github.navikt.tbd_libs.naisful.test.naisfulTestApp
import io.ktor.server.application.Application
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import no.nav.security.mock.oauth2.MockOAuth2Server

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

        return naisfulTestApp(
            testApplicationModule = moduleFunction,
            meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
            objectMapper = objectMapper,
        ) {
            test()
        }
    }
}
