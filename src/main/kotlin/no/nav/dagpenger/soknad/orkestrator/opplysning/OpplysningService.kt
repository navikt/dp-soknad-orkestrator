package no.nav.dagpenger.soknad.orkestrator.opplysning

import no.nav.dagpenger.soknad.orkestrator.opplysning.db.OpplysningRepository
import java.util.UUID

class OpplysningService(private val repository: OpplysningRepository) {
    fun hentOpplysning(
        beskrivendeId: String,
        ident: String,
        søknadsId: String,
    ): Opplysning {
        return repository.hent(
            beskrivendeId = beskrivendeId,
            ident = ident,
            søknadsId = UUID.fromString(søknadsId),
        )
    }
}
