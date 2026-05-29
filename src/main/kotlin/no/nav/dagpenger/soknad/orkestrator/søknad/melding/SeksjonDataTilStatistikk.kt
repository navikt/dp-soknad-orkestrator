package no.nav.dagpenger.soknad.orkestrator.søknad.melding

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import java.time.LocalDateTime
import java.util.UUID

class SeksjonDataTilStatistikk(
    private val søknadId: UUID,
    private val ident: String,
    private val seksjonId: String,
    private val opprettet: LocalDateTime,
    private val oppdatert: LocalDateTime,
) {
    fun asMessage(): JsonMessage =
        JsonMessage.newMessage(
            eventName = "paabegynt_soknad_seksjon",
            map =
                mapOf(
                    "søknad_uuid" to søknadId,
                    "ident" to ident,
                    "seksjon_id" to seksjonId,
                    "opprettet" to opprettet.toString(),
                    "oppdatert" to oppdatert.toString(),
                ),
        )
}
