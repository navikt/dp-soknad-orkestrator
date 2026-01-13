package no.nav.dagpenger.soknad.orkestrator.`søknad`.melding

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import no.nav.dagpenger.soknad.orkestrator.søknad.Dokument
import no.nav.dagpenger.soknad.orkestrator.søknad.Søknad
import java.util.UUID

class MeldingOmEttersending(
    private val søknadId: UUID,
    private val ident: String,
    private val søknad: Søknad,
    private val dokumentKravene: List<Dokument>,
) {
    fun asMessage(): JsonMessage =
        JsonMessage.newMessage(
            søknadId = søknadId,
            ident = ident,
            dokumenter = dokumentKravene,
        )
}
