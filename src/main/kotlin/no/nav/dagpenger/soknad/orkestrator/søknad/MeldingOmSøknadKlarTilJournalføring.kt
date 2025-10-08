package no.nav.dagpenger.soknad.orkestrator.søknad

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import java.time.LocalDateTime
import java.util.UUID

class MeldingOmSøknadKlarTilJournalføring(
    private val søknadId: UUID,
    private val ident: String,
) {
    fun asMessage(): JsonMessage =
        JsonMessage.newMessage(
            eventName = "søknad_klar_til_journalføring",
            map =
                mapOf(
                    "søknadId" to søknadId.toString(),
                    "ident" to ident,
                    "innsendtTidspunkt" to LocalDateTime.now(),
                ),
        )
}
