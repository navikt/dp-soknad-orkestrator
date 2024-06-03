package no.nav.dagpenger.soknad.orkestrator.behov

import no.nav.dagpenger.soknad.orkestrator.utils.asUUID
import no.nav.helse.rapids_rivers.JsonMessage

class Behovmelding(packet: JsonMessage) {
    val behov = packet["@behov"].map { it.asText() }
    val ident = packet["ident"].asText()
    val søknadId = packet["søknadId"].asUUID()
    val innkommendePacket: JsonMessage = packet
}
