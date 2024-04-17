package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory
import no.nav.helse.rapids_rivers.JsonMessage
import java.util.UUID

fun lagPacket(
    ident: String,
    søknadId: UUID,
    behov: BehovløserFactory.Behov,
) = JsonMessage.newMessage(
    eventName = "@behov",
    map =
        mapOf(
            "ident" to ident,
            "søknad_id" to søknadId,
            "@behov" to listOf(behov),
        ),
).apply { this.requireKey("ident", "søknad_id") }
