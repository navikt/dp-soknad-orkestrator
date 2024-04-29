package no.nav.dagpenger.soknad.orkestrator.meldinger

import no.nav.dagpenger.soknad.orkestrator.utils.asUUID
import no.nav.helse.rapids_rivers.JsonMessage
import java.util.UUID

class Behovmelding(packet: JsonMessage) {
    internal val id: UUID = UUID.fromString(packet["@id"].asText())
    val behov = packet["@behov"].map { it.asText() }
    val ident = packet["ident"].asText()
    val søknadId = packet["søknadId"].asUUID()
    val innkommendePacket: JsonMessage = packet
}
