package no.nav.dagpenger.soknad.orkestrator.søknad

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime
import java.util.UUID

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
    val opprettet: LocalDateTime,
    @JsonProperty("søknad_uuid")
    val søknadUUID: UUID,
    val seksjoner: List<Seksjoner>,
)

data class LegacySøknad(
    @JsonProperty("@id")
    val id: UUID,
    @JsonProperty("@opprettet")
    val opprettet: LocalDateTime,
    @JsonProperty("fødselsnummer")
    val ident: String,
    val journalpostId: String,
    val søknadsData: SøknadsData,
)
