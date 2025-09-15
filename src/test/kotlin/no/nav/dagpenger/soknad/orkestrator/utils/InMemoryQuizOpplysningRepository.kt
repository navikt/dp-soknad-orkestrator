package no.nav.dagpenger.soknad.orkestrator.utils

import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.QuizOpplysning
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.asListOf
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Barn
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.BarnSvar
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepository
import java.util.UUID

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

    override fun oppdaterBarn(
        søknadId: UUID,
        oppdatertBarn: BarnSvar,
    ) {
        val opprinneligBarnOpplysning: QuizOpplysning<List<BarnSvar>> =
            hentAlle(søknadId).find { it.type == Barn } as QuizOpplysning<List<BarnSvar>>?
                ?: throw IllegalArgumentException("Fant ikke opplysning om barn for søknad med id $søknadId")

        val oppdatertBarnSvarListe: List<BarnSvar> =
            opprinneligBarnOpplysning.svar.asListOf<BarnSvar>().map {
                if (it.barnSvarId == oppdatertBarn.barnSvarId) oppdatertBarn else it
            }

        val oppdatertOpplysning = opprinneligBarnOpplysning.copy(svar = oppdatertBarnSvarListe)

        opplysninger.remove(opprinneligBarnOpplysning)
        opplysninger.add(oppdatertOpplysning)
    }

    override fun lagreBarnSøknadMapping(
        søknadId: UUID,
        søknadbarnId: UUID,
    ) {
        barnSøknadMapper[søknadId] = søknadbarnId
    }

    override fun hentEllerOpprettSøknadbarnId(søknadId: UUID): UUID =
        barnSøknadMapper.getOrElse(søknadId) {
            val nySøknadbarnId = UUID.randomUUID()
            barnSøknadMapper[søknadId] = nySøknadbarnId

            nySøknadbarnId
        }

    override fun mapTilSøknadId(søknadbarnId: UUID): UUID? = barnSøknadMapper.entries.find { it.value == søknadbarnId }?.key
}
