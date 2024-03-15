package no.nav.dagpenger.soknad.orkestrator.behov

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import java.util.UUID

class BehovMottak(
    rapidsConnection: RapidsConnection,
    private val behovLøsere: List<Behovsløser>,
) : River.PacketListener {
    private val løserBehov = behovLøsere.map { it.behov }

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "behov") }
            validate { it.demandAllOrAny("@behov", løserBehov) }
            validate { it.requireKey("ident", "søknad_id", "behandling_id") }
            validate { it.rejectKey("@løsning") }
        }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        with(packet) {
            val ident = get("ident").asText()
            val søknadsId = UUID.fromString(get("søknad_id").asText())
            val behandlingsId = UUID.fromString(get("behandling_id").asText())

            val mottatteBehov: List<String> = get("@behov").map { it.asText() }.filter { it in løserBehov }

            mottatteBehov.forEach { behov ->
                val behovLøser =
                    behovLøsere.find { it.behov == behov }
                        ?: throw IllegalArgumentException("Kan ikke løse behov: $behov")
                behovLøser.løs(ident, søknadsId, behandlingsId)
            }
        }
    }
}
