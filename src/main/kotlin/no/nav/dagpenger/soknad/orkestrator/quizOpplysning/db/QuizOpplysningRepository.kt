package no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db

import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.QuizOpplysning
import java.util.UUID

interface QuizOpplysningRepository {
    fun lagre(opplysning: QuizOpplysning<*>)

    fun hent(
        beskrivendeId: String,
        ident: String,
        søknadId: UUID,
    ): QuizOpplysning<*>?

    fun hentAlle(søknadId: UUID): List<QuizOpplysning<*>>

    fun slett(søknadId: UUID)
}
