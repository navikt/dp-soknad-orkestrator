package no.nav.søknad

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.objectMapper
import no.nav.opplysning.Opplysning

class SøknadMottak(rapidsConnection: RapidsConnection) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "innsending_ferdigstilt") }
            validate { it.demandAny("type", listOf("NySøknad")) }
            validate { it.requireKey("fødselsnummer") }
            validate { it.requireKey("søknadsData") }
            validate { it.interestedIn("@id", "@opprettet") }
        }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val søknad = objectMapper.readValue(packet.toJson(), Søknad::class.java)
        val opplysninger =
            søknad.søknadsData.seksjoner.map { seksjon ->
                seksjon.fakta.map { fakta ->
                    Opplysning(fakta.svar, fakta.beskrivendeId)
                }
            }.toList()
    }
}
