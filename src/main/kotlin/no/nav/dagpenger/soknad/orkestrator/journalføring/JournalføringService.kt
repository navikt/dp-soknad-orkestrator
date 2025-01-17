package no.nav.dagpenger.soknad.orkestrator.journalføring

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import mu.KotlinLogging
import java.util.Base64
import java.util.UUID

class JournalføringService {
    private lateinit var rapidsConnection: RapidsConnection

    fun setRapidsConnection(rapidsConnection: RapidsConnection) {
        this.rapidsConnection = rapidsConnection
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
        private val sikkerlogg = KotlinLogging.logger("tjenestekall.JournalføringService")
    }

    fun sendJournalførMinidialogBehov(
        ident: String,
        søknadId: UUID,
        dialogId: UUID,
        pdf: ByteArray,
        json: String,
    ) {
        // TODO: Finn riktig brevkode
        val skjemakode = "04-01.03"

        logger.info("Sender journalføringsbehov for minidialog $dialogId, søknad $søknadId")

        val behovNavn = "JournalføreMinidialog"
        val behovParams =
            mapOf(
                "skjemakode" to skjemakode,
                "dialog_uuid" to dialogId,
                "tittel" to "Minidialog",
                "json" to json,
                "pdf" to Base64.getEncoder().encodeToString(pdf),
            )

        val behov =
            JsonMessage.newNeed(
                listOf(behovNavn),
                mapOf(
                    "ident" to ident,
                    "søknad_uuid" to søknadId,
                    behovNavn to behovParams,
                ),
            )

        try {
            rapidsConnection.publish(ident, behov.toJson())
        } catch (e: Exception) {
            logger.error("Kunne ikke sende journalføringsbehov for dialog $dialogId, søknad $søknadId", e)

            throw Exception(e)
        }
    }
}
