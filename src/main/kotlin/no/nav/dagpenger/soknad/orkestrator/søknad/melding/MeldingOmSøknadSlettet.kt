package no.nav.dagpenger.soknad.orkestrator.søknad.melding

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import java.util.UUID

class MeldingOmSøknadSlettet(
    private val søknadId: UUID,
    private val ident: String,
) {
    fun asMessage(): JsonMessage =
        JsonMessage.newMessage(
            eventName = "søknad_slettet",
            map =
                mapOf(
                    "søknad_uuid" to søknadId.toString(),
                    "ident" to ident,
                ),
        )
}
