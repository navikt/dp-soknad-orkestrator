package no.nav.dagpenger.soknad.orkestrator.søknad.melding

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import java.time.LocalDateTime
import java.util.UUID

data class DokumentType(
    val dokumentnavn: String,
    val skjemakode: String,
    val valg: String,
)

class DokumentKravInnsendtMelding(
    private val søknadId: UUID,
    private val ident: String,
    private val dokumenter: List<String>,
) {
    fun asMessage(): JsonMessage =
        JsonMessage.newMessage(
            eventName = "dokumentkrav_innsendt",
            map =
                mapOf(
                    "søknad_uuid" to søknadId,
                    "ident" to ident,
                    "søknadType" to "Dagpenger", // TODO
                    "innsendingsType" to "Søknad", // TODO
                    "innsendttidspunkt" to LocalDateTime.now(),
                    "ferdigBesvart" to true, // TODO
                    "hendelseId" to UUID.randomUUID(),
                    "dokumentkrav" to lagDokumentKravListe(),
                ),
        )

    fun lagDokumentKravListe(): List<DokumentType> {
        if (dokumenter.isEmpty()) return emptyList()
        val dokumentkravList =
            dokumenter
                .map { dokument ->
                    {
                        val dokumentkravForSeksjon = objectMapper.readTree(dokument).toList()
                        dokumentkravForSeksjon.map {
                            DokumentType(
                                dokumentnavn = it.get("type").asText(),
                                skjemakode = it.get("skjemakode").asText(),
                                valg = mapSvaret(it.get("svar").asText()),
                            )
                        }
                    }
                }.flatMap { it() }
        return dokumentkravList
    }

    fun mapSvaret(svar: String) =
        when (svar) {
            "dokumentkravSvarSendNå" -> "SEND_NÅ"
            "dokumentkravSvarSenderIkke" -> "SENDER_IKKE"
            "dokumentkravSvarSenderSenere" -> "SEND_SENERE"
            "dokumentkravSvarSendtTidligere" -> "SEND_TIDLIGERE"
            "dokumentkravEttersendt" -> "ETTERSENDT"
            else -> svar
        }
}
