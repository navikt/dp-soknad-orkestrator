package no.nav.dagpenger.soknad.orkestrator.søknadmottak

import com.fasterxml.jackson.annotation.JsonProperty

data class SøknadsData(
    @JsonProperty("@opprettet")
    val opprettet: String,
    val antallSeksjoner: Int,
    val ferdig: Boolean,
    val fødselsnummer: String,
    val seksjoner: List<Seksjoner>,
    val søknad_uuid: String,
    val versjon_id: Int,
    val versjon_navn: String,
)
