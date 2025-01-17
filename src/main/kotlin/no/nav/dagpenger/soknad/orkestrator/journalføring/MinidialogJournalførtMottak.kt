package no.nav.dagpenger.soknad.orkestrator.journalføring

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import mu.withLoggingContext

internal class MinidialogJournalførtMottak(
    rapidsConnection: RapidsConnection,
) : River.PacketListener {
    private companion object {
        private val logger = KotlinLogging.logger {}
        private val sikkerlogg = KotlinLogging.logger("tjenestekall.MinidialogJournalførtMottak")
    }

    private val behov = "JournalføreMinidialog"

    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireValue("@event_name", "behov") }
                precondition { it.requireAll("@behov", listOf(behov)) }
                validate { it.requireKey("@løsning") }
                validate { it.requireValue("@final", true) } // TODO: Trenger vi dette?
                validate {
                    it.require("@løsning") { løsning ->
                        løsning.required(behov)
                    }
                }
                validate {
                    it.require(behov) { behov ->
                        behov.required("skjemakode")
                        behov.required("dialog_uuid")
                        behov.required("tittel")
                        behov.required("json")
                        behov.required("pdf")
                    }
                }
                validate { it.interestedIn("@id", "@opprettet", "@behovId", "søknad_uuid") } // TODO: Hva trenger vi her?
            }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val behovId = packet["@behovId"].asText()
        val søknadId = packet["søknad_uuid"].asText()
        val dialogId = packet[behov]["dialog_uuid"].asText()
        val journalpostId = packet["@løsning"][behov].asLong()

        runBlocking {
            launch {
                delay(1000)
                println("world")
            }
            println("hello")
        }

        withLoggingContext(
            "behovId" to behovId,
            "søknadId" to søknadId,
            "dialogId" to dialogId,
        ) {
            if (packet["@id"].asText() in emptyList<String>()) {
                logger.warn {
                    "Ignorerer melding fordi den inneholder en duplikat journalføring journalpostId=$journalpostId"
                }
                return
            }

            logger.info { "Fått løsning for JournalføreMinidialog for søknad=$søknadId, dialog=$dialogId, journalpostId=$journalpostId" }
            sikkerlogg.info {
                "Fått løsning for JournalføreMinidialog for søknad=$søknadId, dialog=$dialogId. Packet: ${packet.toJson()}"
            }
        }
    }

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
        metadata: MessageMetadata,
    ) {
        logger.error { problems.toExtendedReport() }
    }
}
