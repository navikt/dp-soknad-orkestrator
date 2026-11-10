package no.nav.dagpenger.soknad.orkestrator.utils

import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.QuizOpplysning
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepository
import java.util.UUID
import java.util.UUID.randomUUID

class InMemoryQuizOpplysningRepository : QuizOpplysningRepository {
    private val opplysninger = mutableListOf<QuizOpplysning<*>>()
    private val barnSøknadMapper = mutableMapOf<UUID, UUID>()

    override fun lagre(opplysning: QuizOpplysning<*>) {
        opplysninger.add(opplysning)
    }

    override fun hent(
        beskrivendeId: String,
        ident: String,
        søknadId: UUID,
    ): QuizOpplysning<*>? =
        opplysninger.find {
            it.beskrivendeId == beskrivendeId && it.ident == ident && it.søknadId == søknadId
        }

    override fun hent(
        beskrivendeId: String,
        søknadId: UUID,
    ): QuizOpplysning<*>? =
        opplysninger.find {
            it.beskrivendeId == beskrivendeId && it.søknadId == søknadId
        }

    override fun hentAlle(søknadId: UUID): List<QuizOpplysning<*>> = opplysninger.filter { it.søknadId == søknadId }

    override fun slett(søknadId: UUID) {
        opplysninger.removeIf { it.søknadId == søknadId }
    }

    override fun lagreBarnSøknadMapping(søknadId: UUID): UUID {
        val søknadbarnId = randomUUID()
        barnSøknadMapper[søknadId] = søknadbarnId

        return søknadbarnId
    }

    override fun hentEllerOpprettSøknadbarnId(søknadId: UUID): UUID =
        barnSøknadMapper.getOrElse(søknadId) {
            val nySøknadbarnId = randomUUID()
            barnSøknadMapper[søknadId] = nySøknadbarnId

            nySøknadbarnId
        }
}
