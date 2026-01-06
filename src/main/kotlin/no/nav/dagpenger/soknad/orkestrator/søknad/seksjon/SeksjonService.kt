package no.nav.dagpenger.soknad.orkestrator.søknad.seksjon

import java.util.UUID

class SeksjonService(
    val seksjonRepository: SeksjonRepository,
) {
    fun lagre(
        søknadId: UUID,
        ident: String,
        seksjonId: String,
        seksjonsvar: String,
        dokumentasjonskrav: String? = null,
        pdfGrunnlag: String,
    ) {
        seksjonRepository.lagre(søknadId, ident, seksjonId, seksjonsvar, dokumentasjonskrav, pdfGrunnlag)
    }

    fun hentSeksjonsvar(
        søknadId: UUID,
        ident: String,
        seksjonId: String,
    ): String? = seksjonRepository.hentSeksjonsvar(søknadId, ident, seksjonId)

    fun hentAlleSeksjonsvar(
        søknadId: UUID,
        ident: String,
    ): List<Seksjon> = seksjonRepository.hentSeksjoner(søknadId, ident)

    fun hentSeksjonIdForAlleLagredeSeksjoner(
        søknadId: UUID,
        ident: String,
    ): List<String> = seksjonRepository.hentSeksjonIdForAlleLagredeSeksjoner(søknadId, ident)

    fun lagreDokumentasjonskrav(
        søknadId: UUID,
        ident: String,
        seksjonId: String,
        dokumentasjonskrav: String? = null,
    ) = seksjonRepository.lagreDokumentasjonskrav(søknadId, ident, seksjonId, dokumentasjonskrav)

    fun hentDokumentasjonskrav(
        søknadId: UUID,
        ident: String,
        seksjonId: String,
    ) = seksjonRepository.hentDokumentasjonskrav(søknadId, ident, seksjonId)

    fun lagreDokumentasjonskravEttersending(
        søknadId: UUID,
        ident: String,
        seksjonId: String,
        dokumentasjonskrav: String? = null,
    ) {
        seksjonRepository.lagreDokumentasjonskravEttersending(søknadId, ident, seksjonId, dokumentasjonskrav)
    }
}

data class Seksjon(
    val seksjonId: String,
    val data: String,
)
