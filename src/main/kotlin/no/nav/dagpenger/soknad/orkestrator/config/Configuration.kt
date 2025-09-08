package no.nav.dagpenger.soknad.orkestrator.config

import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.overriding

internal object Configuration {
    private val defaultProperties =
        ConfigurationMap(
            mapOf(
                "RAPID_APP_NAME" to "dp-soknad-orkestrator",
                "KAFKA_CONSUMER_GROUP_ID" to "dp-soknad-orkestrator-v1",
                "KAFKA_RAPID_TOPIC" to "teamdagpenger.rapid.v1",
                "KAFKA_RESET_POLICY" to "LATEST",
                "KAFKA_BOOTSTRAP_SERVERS" to "127.0.0.1:9092",
            ),
        )

    private val properties =
        ConfigurationProperties.systemProperties() overriding EnvironmentVariables() overriding defaultProperties

    val config: Map<String, String> =
        properties
            .list()
            .reversed()
            .fold(emptyMap()) { map, pair ->
                map + pair.second
            }
}
