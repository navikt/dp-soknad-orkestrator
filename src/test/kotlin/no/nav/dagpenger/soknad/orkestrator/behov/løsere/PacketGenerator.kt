package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory
import no.nav.dagpenger.soknad.orkestrator.meldinger.Behovmelding
import no.nav.helse.rapids_rivers.JsonMessage
import java.util.UUID

fun lagBehovmelding(
    ident: String,
    søknadId: UUID,
    behov: BehovløserFactory.Behov,
): Behovmelding =
    Behovmelding(
        JsonMessage.newMessage(
            eventName = "behov",
            map =
                mapOf(
                    "ident" to ident,
                    "søknadId" to søknadId,
                    "@behov" to listOf(behov),
                ),
        ).apply { this.requireKey("ident", "søknadId", "@behov") },
    )
