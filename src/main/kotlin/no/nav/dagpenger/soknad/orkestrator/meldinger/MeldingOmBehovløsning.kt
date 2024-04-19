package no.nav.dagpenger.soknad.orkestrator.meldinger

import no.nav.helse.rapids_rivers.JsonMessage

class MeldingOmBehovløsning(
    private val behovmelding: Behovmelding,
    private val løsning: Map<String, Any>,
) {
    fun asMessage(): JsonMessage =
        JsonMessage.newMessage(
            eventName = "behov",
            map =
                mapOf(
                    "id" to behovmelding.id,
                    "ident" to behovmelding.ident,
                    "søknad_id" to behovmelding.søknadId,
                    "@behov" to behovmelding.behov,
                    "@løsning" to løsning,
                ),
        )
}
