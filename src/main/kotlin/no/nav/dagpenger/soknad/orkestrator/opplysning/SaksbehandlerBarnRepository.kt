package no.nav.dagpenger.soknad.orkestrator.opplysning

import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.BarnSvar
import java.util.UUID

interface SaksbehandlerBarnRepository {
    fun hentBarn(søknadId: UUID): List<BarnSvar>?

    fun lagreBarn(
        søknadId: UUID,
        barn: List<BarnSvar>,
        endretAv: String,
    )
}
