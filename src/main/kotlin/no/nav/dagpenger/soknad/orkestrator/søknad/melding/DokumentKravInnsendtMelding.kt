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
    private val ferdigBesvart: Boolean,
) {
    val alleDokumentasjonskrav = lagDokumentKravListe()

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
                    "ferdigBesvart" to ferdigBesvart,
                    "hendelseId" to UUID.randomUUID(),
                    "kilde" to "orkestrator",
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
                                dokumentnavn = it.get("type").asString(),
                                skjemakode = it.get("skjemakode").asString(),
                                valg = mapSvaret(it.get("svar").asString()),
                                begrunnelse = it.get("begrunnelse")?.asString(),
                                filer =
                                    it.get("filer")?.values()?.mapNotNull { fil ->
                                        if (fil.isTextual) fil.textValue() else null
                                    } ?: emptyList(),
                                bundle = it.get("bundle")?.toString(),
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
