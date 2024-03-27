package no.nav.dagpenger.soknad.orkestrator.opplysning

import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.Datatype
import java.util.UUID

data class Opplysning<T>(
    val beskrivendeId: String,
    val type: Datatype<T>,
    val svar: T,
    val ident: String,
    val søknadsId: UUID,
)
