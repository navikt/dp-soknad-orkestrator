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
                "PERSON_KONTO_REGISTER_URL" to "http://sokos-kontoregister-person.okonomi/api/borger/v1/hent-aktiv-konto",
            ),
        )

    val properties =
        ConfigurationProperties.systemProperties() overriding EnvironmentVariables() overriding defaultProperties

    val pdlApiUrl by lazy {
        properties[Key("PDL_API_HOST", stringType)].let {
            "https://$it/graphql"
        }
    }
    val pdlApiUserScope by lazy { properties[Key("PDL_API_USER_SCOPE", stringType)] }
    val pdlApiSystemScope by lazy { properties[Key("PDL_API_SYSTEM_SCOPE", stringType)] }
    val personKontoRegisterUrl by lazy { properties[Key("PERSON_KONTO_REGISTER_URL", stringType)] }
    val personKontoRegisterScope by lazy { properties[Key("PERSON_KONTO_REGISTER_SCOPE", stringType)] }

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

    val tokenXClient: CachedOauth2Client by lazy {
        val tokenX = OAuth2Config.TokenX(properties)
        CachedOauth2Client(
            tokenEndpointUrl = tokenX.tokenEndpointUrl,
            authType = tokenX.privateKey(),
        )
    }

    fun tokenXClient(audience: String) =
        { subjectToken: String ->
            tokenXClient
                .tokenExchange(
                    token = subjectToken,
                    audience = audience,
                ).access_token ?: throw RuntimeException("Klarte ikke hente token")
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
