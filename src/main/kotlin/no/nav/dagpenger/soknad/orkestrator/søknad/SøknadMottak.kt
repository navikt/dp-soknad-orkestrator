package no.nav.dagpenger.soknad.orkestrator.søknad

import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import no.nav.dagpenger.soknad.orkestrator.opplysning.db.OpplysningRepository
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

class SøknadMottak(
    rapidsConnection: RapidsConnection,
    private val søknadService: SøknadService,
    private val opplysningRepository: OpplysningRepository,
) :
    River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "søknad_innsendt") }
            validate { it.requireKey("ident", "søknadId", "søknadstidspunkt", "søknadData") }
            validate { it.interestedIn("@id", "@opprettet") }
        }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val jsonNode = objectMapper.readTree(packet.toJson())
        SøknadMapper(jsonNode).søknad
            .also { it.opplysninger.forEach(opplysningRepository::lagre) }
            .also(søknadService::publiserMeldingOmNySøknad)
    }
}
