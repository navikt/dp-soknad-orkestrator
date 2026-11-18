package no.nav.dagpenger.soknad.orkestrator.behov

import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov
import no.nav.dagpenger.soknad.orkestrator.behov.permittering.PermittertFiskeforedlingVurdering
import no.nav.dagpenger.soknad.orkestrator.behov.permittering.PermittertVurdering
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonRepository
import java.util.UUID

class SynkronBehovløser(
    opplysningRepository: QuizOpplysningRepository,
    seksjonRepository: SeksjonRepository,
) {
    private val permittertVurdering = PermittertVurdering(opplysningRepository, seksjonRepository)
    private val permittertFiskeforedlingVurdering =
        PermittertFiskeforedlingVurdering(opplysningRepository, seksjonRepository)

    fun løs(
        behov: Behov,
        ident: String,
        søknadId: UUID,
    ): Boolean =
        when (behov) {
            Behov.Permittert -> permittertVurdering.rettTilDagpenger(ident, søknadId)
            Behov.PermittertFiskeforedling -> permittertFiskeforedlingVurdering.rettTilDagpenger(ident, søknadId)
            else -> throw IkkeStøttetBehov(behov)
        }
}

class IkkeStøttetBehov(
    behov: Behov,
) : RuntimeException("$behov kan ikke løses synkront")
