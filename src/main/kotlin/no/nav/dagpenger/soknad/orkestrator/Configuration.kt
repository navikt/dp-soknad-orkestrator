package no.nav.dagpenger.soknad.orkestrator

import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.PropertyGroup
import com.natpryce.konfig.getValue
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType

internal object Configuration {
    const val APP_NAME = "dp-soknad-orkestrator"
    private val defaultProperties =
        ConfigurationMap(
            emptyMap(),
        )

    val properties =
        ConfigurationProperties.systemProperties() overriding EnvironmentVariables() overriding defaultProperties

    object Grupper : PropertyGroup() {
        val saksbehandler by stringType
    }
}
