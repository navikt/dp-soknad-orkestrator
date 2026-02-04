package no.nav.dagpenger.soknad.orkestrator.søknad.behov

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import no.nav.dagpenger.soknad.orkestrator.søknad.Dokument
import no.nav.dagpenger.soknad.orkestrator.søknad.Dokumentvariant
import no.nav.dagpenger.soknad.orkestrator.søknad.NyJournalpost
import java.util.UUID

class BehovForJournalføringAvSøknadPdfOgVedlegg(
    private val søknadId: UUID,
    private val ident: String,
    private val dokumentvarianter: List<Dokumentvariant>,
    private val dokumenter: List<Dokument> = emptyList(),
    private val skjemakode: String,
) {
    companion object {
        const val BEHOV = "journalfør_søknad_pdf_og_vedlegg"
    }

    fun asMessage(): JsonMessage =
        JsonMessage.newNeed(
            behov = listOf(BEHOV),
            map =
                mapOf(
                    "søknadId" to søknadId.toString(),
                    "ident" to ident,
                    "type" to "NY_DIALOG",
                    BEHOV to
                        NyJournalpost(
                            hovedDokument =
                                Dokument(
                                    skjemakode = skjemakode,
                                    varianter = dokumentvarianter,
                                ),
                            dokumenter = dokumenter,
                        ),
                ),
        )
}
