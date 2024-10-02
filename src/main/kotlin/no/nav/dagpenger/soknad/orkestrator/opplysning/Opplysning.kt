package no.nav.dagpenger.soknad.orkestrator.opplysning

import no.nav.dagpenger.soknad.orkestrator.opplysning.grupper.Seksjonsnavn
import java.util.UUID

data class Opplysning(
    val opplysningId: UUID,
    val seksjonsnavn: Seksjonsnavn,
    val opplysningsbehovId: Int,
    val type: Opplysningstype,
    val svar: Svar<*>?,
)
