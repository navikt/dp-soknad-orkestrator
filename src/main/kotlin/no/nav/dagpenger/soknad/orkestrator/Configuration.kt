package no.nav.dagpenger.soknad.orkestrator

import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.PropertyGroup
import com.natpryce.konfig.getValue
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType
import no.nav.dagpenger.oauth2.CachedOauth2Client
import no.nav.dagpenger.oauth2.OAuth2Config

internal object Configuration {
    const val APP_NAME = "dp-soknad-orkestrator"
    private val defaultProperties =
        ConfigurationMap(
            mapOf(
                "DP_BEHANDLING_BASE_URL" to "http://dp-behandling",
                "DP_BEHANDLING_SCOPE" to "api://dev-gcp.teamdagpenger.dp-behandling/.default",
            ),
        )

    val properties =
        ConfigurationProperties.systemProperties() overriding EnvironmentVariables() overriding defaultProperties

    val miljøVariabler =
        Variabler(
            dpBehandlingBaseUrl = properties[Key("DP_BEHANDLING_BASE_URL", stringType)],
            dpBehandlingScope = properties[Key("DP_BEHANDLING_SCOPE", stringType)],
        )

    object Grupper : PropertyGroup() {
        val saksbehandler by stringType
    }

    val azureAdClient: CachedOauth2Client by lazy {
        val azureAdConfig = OAuth2Config.AzureAd(properties)
        CachedOauth2Client(
            tokenEndpointUrl = azureAdConfig.tokenEndpointUrl,
            authType = azureAdConfig.clientSecret(),
        )
    }
}

data class Variabler(
    val dpBehandlingBaseUrl: String,
    val dpBehandlingScope: String,
) {
    init {
        require(dpBehandlingScope.isNotBlank()) { "DP_BEHANDLING_SCOPE er ikke satt" }
        require(dpBehandlingBaseUrl.isNotBlank()) { "DP_BEHANDLING_BASE_URL er ikke satt" }
    }
}
