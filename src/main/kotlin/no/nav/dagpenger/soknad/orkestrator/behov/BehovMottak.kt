package no.nav.dagpenger.soknad.orkestrator.behov

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import java.util.UUID

class BehovMottak(
    val rapidsConnection: RapidsConnection,
    private val behovLøserFactory: BehovløserFactory,
) : River.PacketListener {
    private val behov = listOf(
        "ØnskerDagpengerFraDato",
        "EøsArbeid",
        "KanJobbeDeltid",
        "HelseTilAlleTyperJobb",
        "KanJobbeHvorSomHelst",
        "VilligTilÅBytteYrke",
        "Søknadstidspunkt",
        "JobbetUtenforNorge",
    )

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "behov") }
            validate { it.demandAllOrAny("@behov", behov) }
            validate { it.requireKey("ident", "søknad_id") }
            validate { it.rejectKey("@løsning") }
            validate { it.interestedIn("behandling_id") }
        }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        with(packet) {
            mottatteBehov().forEach { behov ->
                behovsløserFor(behov).løs(ident(), søknadsId())
            }
        }
    }

    private fun behovsløserFor(behov: String) = behovLøserFactory.behovløserFor(behov)

    private fun JsonMessage.søknadsId(): UUID = UUID.fromString(get("søknad_id").asText())

    private fun JsonMessage.ident(): String = get("ident").asText()

    private fun JsonMessage.mottatteBehov() = get("@behov").map { it.asText() }


}
