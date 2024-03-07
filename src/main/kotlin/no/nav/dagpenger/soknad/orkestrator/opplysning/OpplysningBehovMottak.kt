package no.nav.dagpenger.soknad.orkestrator.opplysning

import com.fasterxml.jackson.databind.JsonNode
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
            validate { it.demand("@behov", behovParser) }
            validate { it.requireKey("ident", "søknad_id", "behandling_id") }
            validate { it.rejectKey("@løsning") }
        }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        with(packet) {
            val beskrivendeId = get("@behov")[0].asText().toBeskrivendeId()
            val ident = get("ident").asText()
            val søknadId = get("søknad_id").asText()
            val behandlingId = get("behandling_id").asText()

            opplysningService.hentOpplysning(
                beskrivendeId = beskrivendeId,
                ident = ident,
                søknadId = søknadId,
                behandlingId = behandlingId,
            ).also(opplysningService::publiserMeldingOmOpplysningBehovLøsning)
        }
    }
}

val behovParser: (JsonNode) -> Unit = { jsonNode ->
    jsonNode[0].asText().let { behovUrn ->
        URN.rfc8141().parse(behovUrn).takeIf {
            it.namespaceIdentifier().toString() == "opplysning"
        } ?: throw IllegalArgumentException("Behov inneholder ikke opplysning: $behovUrn")
    }
}

fun String.toBeskrivendeId() = URN.rfc8141().parse(this).namespaceSpecificString().toString()
