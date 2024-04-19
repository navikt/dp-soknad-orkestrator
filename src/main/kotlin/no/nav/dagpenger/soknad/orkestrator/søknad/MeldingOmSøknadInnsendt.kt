package no.nav.dagpenger.soknad.orkestrator.søknad

import no.nav.helse.rapids_rivers.JsonMessage
import java.util.UUID

class MeldingOmSøknadInnsendt(private val søknadId: UUID, private val ident: String) {
    fun asMessage(): JsonMessage =
        JsonMessage.newMessage(
            eventName = "søknad_innsendt",
            map =
                mapOf(
                    "søknad_id" to søknadId.toString(),
                    "ident" to ident,
                ),
        )
}
