package no.nav.dagpenger.soknad.orkestrator.opplysning

import no.nav.helse.rapids_rivers.JsonMessage

class MeldingOmOpplysningBehovLøsning(private val opplysning: Opplysning) {
    fun asMessage(): JsonMessage =
        JsonMessage.newMessage(
            eventName = "behov",
            map =
                mapOf(
                    "ident" to opplysning.ident,
                    "søknad_id" to opplysning.søknadsId.toString(),
                    "behandling_id" to opplysning.behandlingsId.toString(),
                    "@behov" to listOf("urn:opplysning:${opplysning.beskrivendeId}"),
                    "@løsning" to
                        mapOf(
                            "urn:opplysning:${opplysning.beskrivendeId}" to
                                mapOf(
                                    "status" to "hypotese",
                                    "verdi" to opplysning.svar.first(),
                                ),
                        ),
                ),
        )
}
