package no.nav.dagpenger.soknad.orkestrator.søknadmottak

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonMappingException
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

class SøknadMottak(rapidsConnection: RapidsConnection, private val søknadMapper: SøknadMapper) : River.PacketListener {
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
        søknadMapper.toSøknad(packet.toLegacySøknad())
    }
}

fun JsonMessage.toLegacySøknad(): LegacySøknad {
    try {
        return objectMapper.readValue(this.toJson(), LegacySøknad::class.java)
    } catch (e: JsonMappingException) {
        throw e
    } catch (e: JsonProcessingException) {
        throw e
    } catch (e: Exception) {
        throw e
    }
}
