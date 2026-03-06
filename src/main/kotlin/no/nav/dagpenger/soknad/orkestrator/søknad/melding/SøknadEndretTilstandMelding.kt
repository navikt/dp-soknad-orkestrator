package no.nav.dagpenger.soknad.orkestrator.søknad.melding

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import no.nav.dagpenger.soknad.orkestrator.søknad.Søknad
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.Seksjon
import java.time.LocalDateTime
import java.util.UUID

class SøknadEndretTilstandMelding(
    private val søknadId: UUID,
    private val ident: String,
    private val forrigeTilstand: String,
    private val nyTilstand: String,
    private val søknadsdata: List<Seksjon> = emptyList(),
    private val søknad: Søknad? = null,
) {
    @Suppress("UNCHECKED_CAST")
    fun asMessage(): JsonMessage {
        var response =
            JsonMessage.newMessage(
                eventName = "søknad_endret_tilstand",
                map =
                    mapOf(
                        "søknad_uuid" to søknadId,
                        "ident" to ident,
                        "forrigeTilstand" to mapTilstandsNavn(forrigeTilstand),
                        "gjeldendeTilstand" to mapTilstandsNavn(nyTilstand),
                        "kilde" to "orkestrator",
                        "søknadsdata" to lagSøknadsdataForStatistikk(),
                    ).filterValues { it != null } as Map<String, Any>,
            )

        return response
    }

    private fun mapTilstandsNavn(tilstand: String): String =
        when (tilstand) {
            "PÅBEGYNT" -> "Påbegynt"
            "INNSENDT" -> "Innsendt"
            "SLETTET_AV_SYSTEM" -> "Slettet"
            else -> tilstand
        }

    private fun lagSøknadsdataForStatistikk(): Map<String, Any>? =
        if (søknadsdata.isEmpty() || søknad == null) {
            null
        } else {
            mapOf(
                "opprettet" to LocalDateTime.now(),
                "innsendt" to (søknad.innsendtTidspunkt?.toString() ?: "null"),
            ) + søknadsdata.associate { it.seksjonId to it.data }
        }
}
