package no.nav.dagpenger.soknad.orkestrator.søknad.melding

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import no.nav.dagpenger.soknad.orkestrator.søknad.Tilstand
import java.time.LocalDateTime
import java.util.UUID

data class Dokumentasjonskrav(
    val dokumentnavn: String,
    val skjemakode: String,
    val valg: String,
    val begrunnelse: String? = null,
    val filer: List<String> = emptyList(),
    val bundle: String? = null,
)

data class DokumentkravIMelding(
    val dokumentnavn: String,
    val skjemakode: String,
    val valg: String,
)

class DokumentKravInnsendtMelding(
    private val søknadId: UUID,
    private val ident: String,
    private val dokumenter: List<String>,
    private val innsendtTidspunkt: LocalDateTime,
    private val tilstand: Tilstand,
) {
    val alleDokumentasjonskrav = lagDokumentKravListe()
    val erFerdigBesvart = erDokumentasjonskravFerdigBesvart()

    private fun erDokumentasjonskravFerdigBesvart(): Boolean {
        alleDokumentasjonskrav.all {
            when (it.valg) {
                "SEND_NÅ", "ETTERSENDT" -> it.filer.isNotEmpty() && it.bundle != null
                "SEND_SENERE", "SEND_TIDLIGERE", "SENDER_IKKE" -> it.begrunnelse != null
                else -> false
            }
        }
        return true
    }

    fun asMessage(): JsonMessage =
        JsonMessage.newMessage(
            eventName = "dokumentkrav_innsendt",
            map =
                mapOf(
                    "søknad_uuid" to søknadId,
                    "ident" to ident,
                    "søknadType" to "Dagpenger",
                    "innsendingsType" to finnInnsendingsType(),
                    "innsendttidspunkt" to innsendtTidspunkt,
                    "ferdigBesvart" to erFerdigBesvart,
                    "hendelseId" to UUID.randomUUID(),
                    "dokumentkrav" to
                        alleDokumentasjonskrav
                            .map {
                                DokumentkravIMelding(
                                    dokumentnavn = it.dokumentnavn,
                                    skjemakode = it.skjemakode,
                                    valg = it.valg,
                                )
                            }.toList(),
                ),
        )

    private fun finnInnsendingsType(): String =
        when (tilstand) {
            Tilstand.INNSENDT -> "INNSENDT"
            Tilstand.JOURNALFØRT -> "ETTERSENDT"
            else -> "UKJENT"
        }

    private fun lagDokumentKravListe(): List<Dokumentasjonskrav> {
        if (dokumenter.isEmpty()) return emptyList()
        val dokumentkravList =
            dokumenter
                .map { dokument ->
                    {
                        val dokumentkravForSeksjon = objectMapper.readTree(dokument).toList()
                        dokumentkravForSeksjon.map {
                            Dokumentasjonskrav(
                                dokumentnavn = it.get("type").asText(),
                                skjemakode = it.get("skjemakode").asText(),
                                valg = mapSvaret(it.get("svar").asText()),
                                begrunnelse = it.get("begrunnelse")?.asText(),
                                filer = it.get("filer")?.map { fil -> fil.asText() }?.toList() ?: emptyList(),
                                bundle = it.get("bundle")?.asText(),
                            )
                        }
                    }
                }.flatMap { it() }
        return dokumentkravList
    }

    private fun mapSvaret(svar: String) =
        when (svar) {
            "dokumentkravSvarSendNå" -> "SEND_NÅ"
            "dokumentkravSvarSenderIkke" -> "SENDER_IKKE"
            "dokumentkravSvarSenderSenere" -> "SEND_SENERE"
            "dokumentkravSvarSendtTidligere" -> "SEND_TIDLIGERE"
            "dokumentkravEttersendt" -> "ETTERSENDT"
            else -> svar
        }
}
