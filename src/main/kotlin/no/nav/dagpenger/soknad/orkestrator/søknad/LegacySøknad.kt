package no.nav.dagpenger.soknad.orkestrator.søknad

import com.fasterxml.jackson.annotation.JsonProperty

data class Fakta(
    val beskrivendeId: String,
    val svar: List<String>,
    val type: String,
)

data class Seksjoner(
    val beskrivendeId: String,
    val fakta: List<Fakta>,
)

data class SøknadsData(
    @JsonProperty("@opprettet")
    val opprettet: String,
    val seksjoner: List<Seksjoner>,
    val søknad_uuid: String,
)

data class LegacySøknad(
    @JsonProperty("@id")
    val id: String,
    @JsonProperty("@opprettet")
    val opprettet: String,
    val fødselsnummer: String,
    val journalpostId: String,
    val søknadsData: SøknadsData,
)
