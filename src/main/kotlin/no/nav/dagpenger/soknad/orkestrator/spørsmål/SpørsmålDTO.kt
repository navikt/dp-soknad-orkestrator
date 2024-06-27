package no.nav.dagpenger.soknad.orkestrator.spørsmål

import java.util.UUID

open class SpørsmålDTO<T>(
    val id: UUID,
    val tekstnøkkel: String,
    val type: SpørsmålType,
    val svar: T? = null,
    val gyldigeSvar: List<String>? = null,
)
