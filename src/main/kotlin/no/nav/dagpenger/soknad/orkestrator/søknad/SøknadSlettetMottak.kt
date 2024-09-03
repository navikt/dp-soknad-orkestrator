package no.nav.dagpenger.soknad.orkestrator.søknad

import mu.KotlinLogging
import no.nav.dagpenger.soknad.orkestrator.utils.asUUID
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.withMDC

class SøknadSlettetMottak(
    rapidsConnection: RapidsConnection,
    private val søknadService: SøknadService,
) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "søknad_slettet") }
            validate { it.requireKey("søknad_uuid", "ident") }
            validate { it.interestedIn("@id", "@opprettet") }
        }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        withMDC(
            mapOf("søknadId" to packet["søknad_uuid"].asText()),
        ) {
            logger.info { "Mottok søknad slettet hendelse for søknad ${packet["søknad_uuid"]}" }
            sikkerlogg.info { "Mottok søknad slettet hendelse: ${packet.toJson()}" }

            val søknadId = packet["søknad_uuid"].asUUID()
            val ident = packet["ident"].asText()

            søknadService.slett(søknadId, ident)
        }
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
        private val sikkerlogg = KotlinLogging.logger("tjenestekall.SøknadMottak")
    }
}
