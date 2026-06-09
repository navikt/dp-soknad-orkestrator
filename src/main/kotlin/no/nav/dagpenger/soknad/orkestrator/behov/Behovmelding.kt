package no.nav.dagpenger.soknad.orkestrator.behov

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import no.nav.dagpenger.soknad.orkestrator.utils.asUUID

open class Behovmelding(
    packet: JsonMessage,
) {
    val behov = packet["@behov"].values().map { it.asString() }
    val ident = packet["ident"].asString()
    val søknadId = packet["søknadId"].asUUID()
    val innkommendePacket: JsonMessage = packet
}

class SøknadBehovmelding(
    packet: JsonMessage,
) {
    val behov = packet["@behov"].values().map { it.asString() }
    val ident = packet["ident"].asString()
    val innkommendePacket: JsonMessage = packet
}
