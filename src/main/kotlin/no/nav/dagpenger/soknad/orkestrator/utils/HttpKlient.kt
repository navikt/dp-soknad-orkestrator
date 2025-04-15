package no.nav.dagpenger.soknad.orkestrator.utils

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.jackson.jackson
import no.nav.dagpenger.soknad.orkestrator.config.configure

fun configureHttpClient() =
    HttpClient(CIO) {
        expectSuccess = true
        install(ContentNegotiation) {
            jackson { configure() }
        }
        install(Logging) {
            level = LogLevel.INFO
        }
    }

val httpKlient = configureHttpClient()
