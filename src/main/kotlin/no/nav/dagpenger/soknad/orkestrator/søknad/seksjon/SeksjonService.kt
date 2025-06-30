package no.nav.dagpenger.soknad.orkestrator.søknad.seksjon

import java.util.UUID

class SeksjonService(
    val seksjonRepository: SeksjonRepository,
) {
    fun lagre(
        søknadId: UUID,
        seksjonId: String,
        json: String,
    ) {
        seksjonRepository.lagre(søknadId, seksjonId, json)
    }
}
