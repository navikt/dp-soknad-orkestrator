package no.nav.søknad

import com.fasterxml.jackson.annotation.JsonProperty

data class Søknad(
    @JsonProperty("@event_name")
    val event_name: String,
    @JsonProperty("@id")
    val id: String,
    @JsonProperty("@opprettet")
    val opprettet: String,
    val aktørId: String,
    val datoRegistrert: String,
    val fødselsnummer: String,
    val journalpostId: String,
    val skjemaKode: String,
    val søknadsData: SøknadsData,
    val tittel: String,
    val type: String,
)
