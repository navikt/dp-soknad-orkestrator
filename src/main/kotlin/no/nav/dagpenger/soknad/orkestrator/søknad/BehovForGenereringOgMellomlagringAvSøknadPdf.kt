package no.nav.dagpenger.soknad.orkestrator.søknad

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import java.util.UUID

class BehovForGenereringOgMellomlagringAvSøknadPdf(
    private val søknadId: UUID,
    private val ident: String,
    private val bruttoPayload: String,
    private val nettoPayload: String,
) {
    fun asMessage(): JsonMessage =
        JsonMessage.newNeed(
            behov = listOf("generer_og_mellomlagre_søknad_pdf"),
            map =
                mapOf(
                    "søknadId" to søknadId.toString(),
                    "ident" to ident,
                    "bruttoPayload" to bruttoPayload,
                    "nettoPayload" to nettoPayload,
                ),
        )
}
