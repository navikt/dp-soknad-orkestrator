package no.nav.dagpenger.soknad.orkestrator.søknad

import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysning
import java.util.UUID

class Søknad(
    val søknadId: UUID,
    val ident: String,
    val opplysninger: List<Opplysning<*>>,
)
