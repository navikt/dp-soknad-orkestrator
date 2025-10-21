package no.nav.dagpenger.soknad.orkestrator.søknad.seksjon

import java.util.UUID

class SeksjonService(
    val seksjonRepository: SeksjonRepository,
) {
    fun lagre(
        ident: String,
        søknadId: UUID,
        seksjonId: String,
        json: String,
    ) {
        seksjonRepository.lagre(ident, søknadId, seksjonId, json)
    }

    fun hent(
        ident: String,
        søknadId: UUID,
        seksjonId: String,
    ): String? = seksjonRepository.hent(ident, søknadId, seksjonId)

    fun hentAlle(
        ident: String,
        søknadId: UUID,
    ): List<Seksjon> = seksjonRepository.hentSeksjoner(ident, søknadId)

    fun hentLagredeSeksjonerForGittSøknadId(
        ident: String,
        søknadId: UUID,
    ): List<String> = seksjonRepository.hentFullførteSeksjoner(ident, søknadId)
}

data class Seksjon(
    val seksjonId: String,
    val data: String,
)
