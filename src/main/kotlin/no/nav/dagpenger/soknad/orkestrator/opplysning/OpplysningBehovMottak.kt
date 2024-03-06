package no.nav.dagpenger.soknad.orkestrator.opplysning

import de.slub.urn.URN
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

class OpplysningBehovMottak(
    rapidsConnection: RapidsConnection,
    private val opplysningService: OpplysningService,
) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "behov") }
            validate {
                it.demand("@behov") { jsonNode ->
                    jsonNode[0].asText().let { behovUrn ->
                        val urn = URN.rfc8141().parse(behovUrn)
                        if (urn.namespaceIdentifier().toString() != "opplysning") {
                            throw IllegalArgumentException("Behov inneholder ikke opplysning: $behovUrn")
                        }
                    }
                }
            }
            validate { it.requireKey("ident", "søknad_id", "behandling_id") }
            validate { it.rejectKey("@løsning") }
        }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val ident = packet["ident"].asText()
        val søknadId = packet["søknad_id"].asText()
        val behandlingId = packet["behandling_id"].asText()

        val beskrivendeId =
            URN.rfc8141()
                .parse(packet["@behov"][0].asText())
                .namespaceSpecificString().toString()

        opplysningService.hentOpplysning(ident, søknadId, behandlingId, beskrivendeId)
    }
}
