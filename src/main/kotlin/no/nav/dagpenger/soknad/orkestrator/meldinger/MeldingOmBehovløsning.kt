package no.nav.dagpenger.soknad.orkestrator.meldinger

import no.nav.helse.rapids_rivers.JsonMessage

class MeldingOmBehovløsning(
    private val behovMelding: BehovMelding,
    private val løsning: Map<String, Any>,
) {
    fun asMessage(): JsonMessage =
        JsonMessage.newMessage(
            eventName = "behov",
            map =
                mapOf(
                    "id" to behovMelding.id,
                    "ident" to behovMelding.ident,
                    "søknad_id" to behovMelding.søknadId,
                    "@behov" to behovMelding.behov,
                    "@løsning" to løsning,
                ),
        )
}
