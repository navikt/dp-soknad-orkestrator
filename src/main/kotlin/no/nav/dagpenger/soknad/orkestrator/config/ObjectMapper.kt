package no.nav.dagpenger.soknad.orkestrator.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule

val objectMapper =
    ObjectMapper().apply {
        registerModules(
            KotlinModule.Builder()
                .withReflectionCacheSize(512)
                .build(),
        )

        registerModules(JavaTimeModule())
        configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
        configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    }
