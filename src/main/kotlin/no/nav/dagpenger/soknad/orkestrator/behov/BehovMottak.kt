package no.nav.dagpenger.soknad.orkestrator.behov

import mu.KotlinLogging
import no.nav.dagpenger.soknad.orkestrator.metrikker.BehovMetrikker
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import java.util.UUID

class BehovMottak(
    val rapidsConnection: RapidsConnection,
    private val behovløserFactory: BehovløserFactory,
) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "behov") }
            validate { it.demandAllOrAny("@behov", behovløserFactory.behov()) }
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
            logger.info { "Mottok behov: ${mottatteBehov()}" }

            mottatteBehov().forEach { behov ->
                BehovMetrikker.mottatt.labels(behov).inc()
                try {
                    behovsløserFor(behov).løs(ident(), søknadId())
                } catch (e: Exception) {
                    logger.error(e) { "Feil ved løsning av behov $behov" }
                }
            }
        }
    }

    internal fun behovsløserFor(behov: String) = behovløserFactory.behovløserFor(behov)

    private fun JsonMessage.søknadId(): UUID = UUID.fromString(get("søknad_id").asText())

    private fun JsonMessage.ident(): String = get("ident").asText()

    private fun JsonMessage.mottatteBehov() = get("@behov").map { it.asText() }

    private companion object {
        private val logger = KotlinLogging.logger {}
    }
}
