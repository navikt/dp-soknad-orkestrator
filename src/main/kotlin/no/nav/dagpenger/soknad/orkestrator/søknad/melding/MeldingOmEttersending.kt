package no.nav.dagpenger.soknad.orkestrator.søknad.melding

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import no.nav.dagpenger.soknad.orkestrator.søknad.Dokument
import no.nav.dagpenger.soknad.orkestrator.søknad.NyEttersendingJournalPost
import java.util.UUID

class MeldingOmEttersending(
    private val søknadId: UUID,
    private val ident: String,
    private val dokumentKravene: List<Dokument>,
    private val dokumentasjonskravJson: String,
    private val seksjonId: String,
) {
    companion object {
        const val BEHOV = "journalfør_ettersending_av_dokumentasjon"
    }

    fun asMessage(): JsonMessage =
        JsonMessage.newNeed(
            behov = listOf(BEHOV),
            map =
                mapOf(
                    "søknadId" to søknadId.toString(),
                    "ident" to ident,
                    "type" to "ETTERSENDING_TIL_DIALOG",
                    BEHOV to
                        NyEttersendingJournalPost(
                            dokumenter = dokumentKravene,
                            dokumentasjonskravJson = dokumentasjonskravJson,
                            seksjonId = seksjonId,
                        ),
                ),
        )
}
