package no.nav.dagpenger.soknad.orkestrator.søknad

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import java.util.UUID

class MeldingOmSøknadInnsendt(
    private val søknadId: UUID,
    private val ident: String,
) {
    fun asMessage(): JsonMessage =
        JsonMessage.newMessage(
            eventName = "søknad_innsendt",
            map =
                mapOf(
                    "søknadId" to søknadId.toString(),
                    "ident" to ident,
                ),
        )
}
