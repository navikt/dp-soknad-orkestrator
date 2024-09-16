package no.nav.dagpenger.soknad.orkestrator.søknad

import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.QuizOpplysning
import java.util.UUID

class Søknad(
    val søknadId: UUID = UUID.randomUUID(),
    val ident: String,
    val opplysninger: List<QuizOpplysning<*>> = emptyList(),
)
