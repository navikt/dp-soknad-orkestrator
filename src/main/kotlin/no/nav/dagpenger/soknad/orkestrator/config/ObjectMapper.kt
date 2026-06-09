package no.nav.dagpenger.soknad.orkestrator.config

import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.databind.introspect.DefaultAccessorNamingStrategy
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.jacksonMapperBuilder

val objectMapper: ObjectMapper =
    jacksonMapperBuilder()
        .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
        .accessorNaming(DefaultAccessorNamingStrategy.Provider().withFirstCharAcceptance(true, true))
        .build()

fun JsonMapper.Builder.configure() {
    enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
    accessorNaming(DefaultAccessorNamingStrategy.Provider().withFirstCharAcceptance(true, true))
}
