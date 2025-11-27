package no.nav.dagpenger.soknad.orkestrator.søknad.seksjon

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
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
        seksjonId: String,
    ) = seksjonRepository.hentDokumentasjonskrav(ident, søknadId, seksjonId)

    fun hentAlleSeksjonerMedSeksjonIdSomNøkkel(
        ident: String,
        søknadId: UUID,
    ): Map<String, String> {
        val hentAlleSeksjoner = seksjonRepository.hentSeksjoner(ident, søknadId)
        return hentAlleSeksjoner.map {
            val seksjonId = it.seksjonId
            val seksjonSvarToJson = Json.parseToJsonElement(it.data).jsonObject
            val seksjonsvar = seksjonSvarToJson["seksjonsvar"]?.toString() ?: ""
            seksjonId to seksjonsvar
        } as Map<String, String>
    }
}

data class Seksjon(
    val seksjonId: String,
    val data: String,
)
