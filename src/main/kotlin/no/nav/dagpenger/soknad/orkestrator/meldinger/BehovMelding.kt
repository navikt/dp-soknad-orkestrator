package no.nav.dagpenger.soknad.orkestrator.meldinger

import no.nav.dagpenger.soknad.orkestrator.utils.asUUID
import no.nav.helse.rapids_rivers.JsonMessage
import java.util.UUID

class BehovMelding(packet: JsonMessage) {
    internal val id: UUID = UUID.fromString(packet["@id"].asText())
    val behov = packet["@behov"].asText().map { it }
    val ident = packet["ident"].asText()
    val søknadId = packet["søknad_id"].asUUID()
}
