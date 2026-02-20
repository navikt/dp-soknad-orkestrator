package no.nav.dagpenger.soknad.orkestrator.søknad.melding

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import java.util.UUID

class SøknadEndretTilstandMelding(
    private val søknadId: UUID,
    private val ident: String,
    private val forrigeTilstand: String,
    private val nyTilstand: String,
) {
    fun asMessage(): JsonMessage =
        JsonMessage.newMessage(
            eventName = "søknad_endret_tilstand",
            map =
                mapOf(
                    "søknad_uuid" to søknadId,
                    "ident" to ident,
                    "forrigeTilstand" to mapTilstandsNavn(forrigeTilstand),
                    "gjeldendeTilstand" to mapTilstandsNavn(nyTilstand),
                ),
        )

    private fun mapTilstandsNavn(tilstand: String): String =
        when (tilstand) {
            "PÅBEGYNT" -> "Påbegynt"
            "INNSENDT" -> "Innsendt"
            "SLETTET_AV_SYSTEM" -> "Slettet"
            else -> tilstand
        }
}
