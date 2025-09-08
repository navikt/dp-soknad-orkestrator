package no.nav.dagpenger.soknad.orkestrator.quizOpplysning

import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Datatype
import java.util.UUID

data class QuizOpplysning<T>(
    val beskrivendeId: String,
    val type: Datatype<T>,
    val svar: T,
    val ident: String,
    val søknadId: UUID,
)

inline fun <reified T> Any?.asListOf(): List<T> = (this as? List<*>)?.filterIsInstance<T>() ?: emptyList()
