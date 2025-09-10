package no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db

import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.QuizOpplysning
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.BarnSvar
import java.util.UUID

interface QuizOpplysningRepository {
    fun lagre(opplysning: QuizOpplysning<*>)

    fun hent(
        beskrivendeId: String,
        ident: String,
        søknadId: UUID,
    ): QuizOpplysning<*>?

    fun hent(
        beskrivendeId: String,
        søknadId: UUID,
    ): QuizOpplysning<*>?

    fun hentAlle(søknadId: UUID): List<QuizOpplysning<*>>

    fun slett(søknadId: UUID)

    fun oppdaterBarn(
        søknadId: UUID,
        oppdatertBarn: BarnSvar,
    )

    fun lagreBarnSøknadMapping(
        søknadId: UUID,
        søknadbarnId: UUID,
    )

    fun mapTilSøknadbarnId(søknadId: UUID): UUID?

    fun mapTilSøknadId(søknadbarnId: UUID): UUID?
}
