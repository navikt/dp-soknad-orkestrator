package no.nav.dagpenger.soknad.orkestrator.søknad

import no.nav.helse.rapids_rivers.JsonMessage
import java.util.UUID

class MeldingOmNySøknad(private val søknadUUID: UUID, private val ident: String) {
    fun asMessage(): JsonMessage =
        JsonMessage.newMessage(
            eventName = "melding_om_ny_søknad",
            map =
                mapOf(
                    "søknad_uuid" to søknadUUID.toString(),
                    "ident" to ident,
                ),
        )
}
