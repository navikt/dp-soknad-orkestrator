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

    fun hent(
        søknadId: UUID,
        seksjonId: String,
    ): String? = seksjonRepository.hent(søknadId, seksjonId)

    fun hentAlle(søknadId: UUID): List<Seksjon> = seksjonRepository.hentSeksjoner(søknadId)
}

data class Seksjon(
    val seksjonId: String,
    val data: String,
)
