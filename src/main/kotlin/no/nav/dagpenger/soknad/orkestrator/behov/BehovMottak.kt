package no.nav.dagpenger.soknad.orkestrator.behov

import mu.KotlinLogging
import no.nav.dagpenger.soknad.orkestrator.metrikker.BehovMetrikker
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.withMDC

class BehovMottak(
    val rapidsConnection: RapidsConnection,
    private val behovløserFactory: BehovløserFactory,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                validate { it.demandValue("@event_name", "behov") }
                validate { it.demandAllOrAny("@behov", behovløserFactory.behov()) }
                validate { it.requireKey("ident", "søknadId", "behandlingId", "@behovId") }
                validate { it.rejectKey("@løsning") }
            }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        withMDC(
            mapOf(
                "søknadId" to packet["søknadId"].asText(),
                "behandlingId" to packet["behandlingId"].asText(),
                "behovId" to packet["@behovId"].asText(),
            ),
        ) {
            logger.info { "Mottok behov: ${packet.mottatteBehov()}" }

            packet.mottatteBehov().forEach { behov ->
                BehovMetrikker.mottatt.labels(behov).inc()
                try {
                    behovsløserFor(BehovløserFactory.Behov.valueOf(behov)).løs(Behovmelding(packet))
                } catch (e: IllegalArgumentException) {
                    logger.error(e) { "Kunne ikke løse behov $behov. Ett eller flere argumenter var feil." }
                } catch (e: IllegalStateException) {
                    logger.error(e) { "Kunne ikke løse behov $behov. Opplysningen finnes ikke." }
                }
            }
        }
    }

    internal fun behovsløserFor(behov: BehovløserFactory.Behov) = behovløserFactory.behovløserFor(behov)

    private companion object {
        private val logger = KotlinLogging.logger {}
    }
}

internal fun JsonMessage.mottatteBehov() =
    get("@behov").map { it.asText() }.filter { behov ->
        BehovløserFactory.Behov.entries.any { it.name == behov }
    }
