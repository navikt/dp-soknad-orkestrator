package no.nav.dagpenger.soknad.orkestrator.behov

import no.nav.helse.rapids_rivers.JsonMessage
import java.util.UUID

class MeldingOmBehovløsning(
    private val ident: String,
    private val søknadId: UUID,
    private val løsning: Map<String, Any>,
) {
    fun asMessage(): JsonMessage =
        JsonMessage.newMessage(
            eventName = "behov",
            map =
                mapOf(
                    "ident" to ident,
                    "søknad_id" to søknadId,
                    "@behov" to listOf(løsning.keys),
                    "@løsning" to løsning,
                ),
        )
}
