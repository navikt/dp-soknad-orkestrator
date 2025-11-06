package no.nav.dagpenger.soknad.orkestrator.behov

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.withMDC
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.soknad.orkestrator.metrikker.BehovMetrikker
import no.nav.dagpenger.soknad.orkestrator.søknad.SøknadService
import no.nav.dagpenger.soknad.orkestrator.utils.asUUID

class BehovMottak(
    val rapidsConnection: RapidsConnection,
    private val behovløserFactory: BehovløserFactory,
    private val søknadService: SøknadService,
) : River.PacketListener {
    private companion object {
        private val logger = KotlinLogging.logger {}
        private val behovIdSkipSet =
            setOf(
                "93fe996c-2ead-4e67-b6dc-cac88cf16954",
            )
    }

    init {
        River(rapidsConnection)
            .apply {
                precondition {
                    it.requireValue("@event_name", "behov")
                    it.requireAllOrAny("@behov", behovløserFactory.behov())
                    it.forbid("@løsning")
                }
                validate {
                    it.requireKey("ident", "søknadId", "behandlingId", "@behovId")
                }
            }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val behovId = packet["@behovId"].asText()

        withMDC(
            mapOf(
                "søknadId" to packet["søknadId"].asText(),
                "behandlingId" to packet["behandlingId"].asText(),
                "behovId" to behovId,
            ),
        ) {
            logger.info { "Mottok behov: ${packet.mottatteBehov()}" }

            if (behovIdSkipSet.contains(behovId)) {
                logger.info { "Mottok behov $behovId som ligger i behovIdSkipSet, ignorerer meldingen." }
                return@withMDC
            }

            if (!søknadService.søknadFinnes(packet["søknadId"].asUUID())) {
                logger.warn { "Søknad med søknadId: ${packet["søknadId"].asText()} finnes ikke, kan ikke løse behov" }
            } else {
                packet.løsBehov()
            }
        }
    }

    private fun JsonMessage.løsBehov() {
        this.mottatteBehov().forEach { behov ->
            BehovMetrikker.mottatt.labelValues(behov).inc()
            try {
                behovsløserFor(BehovløserFactory.Behov.valueOf(behov)).løs(Behovmelding(this))
            } catch (e: IllegalArgumentException) {
                logger.error(e) { "Kunne ikke løse behov $behov. Ett eller flere argumenter var feil." }
            } catch (e: IllegalStateException) {
                logger.error(e) { "Kunne ikke løse behov $behov. Opplysningen finnes ikke." }
            }
        }
    }

    internal fun behovsløserFor(behov: BehovløserFactory.Behov) = behovløserFactory.behovløserFor(behov)
}

internal fun JsonMessage.mottatteBehov() =
    get("@behov").map { it.asText() }.filter { behov ->
        BehovløserFactory.Behov.entries.any { it.name == behov }
    }
