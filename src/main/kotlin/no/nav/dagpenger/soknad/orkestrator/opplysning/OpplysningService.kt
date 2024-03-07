package no.nav.dagpenger.soknad.orkestrator.opplysning

import java.util.UUID

class OpplysningService(private val repository: OpplysningRepository) {
    fun hentOpplysning(
        beskrivendeId: String,
        ident: String,
        søknadId: String,
        behandlingId: String,
    ): Opplysning {
        return repository.hent(
            beskrivendeId,
            ident,
            UUID.fromString(søknadId),
        )
    }
}
