package no.nav.dagpenger.soknad.orkestrator.utils

import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.QuizOpplysning
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepository
import java.util.UUID

class InMemoryQuizOpplysningRepository : QuizOpplysningRepository {
    private val opplysninger = mutableListOf<QuizOpplysning<*>>()

    override fun lagre(opplysning: QuizOpplysning<*>) {
        opplysninger.add(opplysning)
    }

    override fun hent(
        beskrivendeId: String,
        ident: String,
        søknadId: UUID,
    ): QuizOpplysning<*>? {
        return opplysninger.find {
            it.beskrivendeId == beskrivendeId && it.ident == ident && it.søknadId == søknadId
        }
    }

    override fun hent(
        beskrivendeId: String,
        søknadId: UUID,
    ): QuizOpplysning<*>? {
        return opplysninger.find {
            it.beskrivendeId == beskrivendeId && it.søknadId == søknadId
        }
    }

    override fun hentAlle(søknadId: UUID): List<QuizOpplysning<*>> {
        return opplysninger.filter { it.søknadId == søknadId }
    }

    override fun slett(søknadId: UUID) {
        opplysninger.removeIf { it.søknadId == søknadId }
    }
}
