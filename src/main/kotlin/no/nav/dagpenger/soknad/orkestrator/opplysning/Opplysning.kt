package no.nav.dagpenger.soknad.orkestrator.opplysning

import java.util.UUID

data class Opplysning(
    val opplysningId: UUID,
    val seksjonversjon: String,
    val opplysningsbehovId: Int,
    val type: Opplysningstype,
    val svar: Svar<*>?,
)
