package no.nav.dagpenger.soknad.orkestrator.søknad.seksjon

import java.util.UUID

class SeksjonService(
    val seksjonRepository: SeksjonRepository,
) {
    fun lagre(
        ident: String,
        søknadId: UUID,
        seksjonId: String,
        seksjonsvar: String,
        pdfGrunnlag: String,
    ) {
        seksjonRepository.lagre(ident, søknadId, seksjonId, seksjonsvar, pdfGrunnlag)
    }

    fun hentSeksjonsvar(
        ident: String,
        søknadId: UUID,
        seksjonId: String,
    ): String? = seksjonRepository.hentSeksjonsvar(ident, søknadId, seksjonId)

    fun hentAlleSeksjonsvar(
        ident: String,
        søknadId: UUID,
    ): List<Seksjon> = seksjonRepository.hentSeksjoner(ident, søknadId)

    fun hentSeksjonIdForAlleLagredeSeksjoner(
        ident: String,
        søknadId: UUID,
    ): List<String> = seksjonRepository.hentSeksjonIdForAlleLagredeSeksjoner(ident, søknadId)

    fun slettAlleSeksjoner(søknadId: UUID) = seksjonRepository.slettAlleSeksjoner(søknadId)
}

data class Seksjon(
    val seksjonId: String,
    val data: String,
)
