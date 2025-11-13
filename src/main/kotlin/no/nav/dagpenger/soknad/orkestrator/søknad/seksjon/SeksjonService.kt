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
        dokumentasjonskrav: String? = null,
        pdfGrunnlag: String,
    ) {
        seksjonRepository.lagre(ident, søknadId, seksjonId, seksjonsvar, dokumentasjonskrav, pdfGrunnlag)
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

    fun lagreDokumentasjonskrav(
        ident: String,
        søknadId: UUID,
        seksjonId: String,
        dokumentasjonskrav: String? = null,
    ) = seksjonRepository.lagreDokumentasjonskrav(ident, søknadId, seksjonId, dokumentasjonskrav)

    fun hentDokumentasjonskrav(
        ident: String,
        søknadId: UUID,
    ) = seksjonRepository.hentDokumentasjonskrav(ident, søknadId)

    fun hentDokumentasjonskrav(
        ident: String,
        søknadId: UUID,
        seksjonId: String,
    ) = seksjonRepository.hentDokumentasjonskrav(ident, søknadId, seksjonId)
}

data class Seksjon(
    val seksjonId: String,
    val data: String,
)
