package no.nav.dagpenger.soknad.orkestrator.behov

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import java.util.UUID

class BehovMottak(
    val rapidsConnection: RapidsConnection,
    private val behovLøserFactory: BehovLøserFactory,
) : River.PacketListener {
    private val behov =
        listOf(
            // "Søknadstidspunkt",
            // "JobbetUtenforNorge",
            "ØnskerDagpengerFraDato",
            "EøsArbeid",
            // "KanJobbeDeltid",
            // "HelseTilAlleTyperJobb",
            // "KanJobbeHvorSomHelst",
            // "VilligTilÅBytteYrke",
        )

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "behov") }
            validate { it.demandAllOrAny("@behov", behov) }
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

            val mottatteBehov = get("@behov").map { it.asText() }

            mottatteBehov.forEach { behov ->
                val behovløser = behovLøserFactory.behovsløser(behov)
                behovløser.løs(ident, søknadsId, behandlingsId)
            }
        }
    }
}
