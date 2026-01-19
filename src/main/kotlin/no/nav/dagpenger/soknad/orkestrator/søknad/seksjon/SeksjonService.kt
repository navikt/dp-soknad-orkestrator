package no.nav.dagpenger.soknad.orkestrator.søknad.seksjon

import com.fasterxml.jackson.databind.json.JsonMapper
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.soknad.orkestrator.søknad.Dokument
import no.nav.dagpenger.soknad.orkestrator.søknad.Dokumentvariant
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.melding.MeldingOmEttersending
import java.util.UUID

class SeksjonService(
    val seksjonRepository: SeksjonRepository,
    val søknadRepository: SøknadRepository,
) {
    private companion object {
        private val logg = KotlinLogging.logger {}
        private val sikkerlogg = KotlinLogging.logger("tjenestekall.SøknadService")
        private val jsonMapper = JsonMapper.builder().build()
    }

    private lateinit var rapidsConnection: RapidsConnection

    fun setRapidsConnection(rapidsConnection: RapidsConnection) {
        this.rapidsConnection = rapidsConnection
    }

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

    fun lagreDokumentasjonskravEttersending(
        søknadId: UUID,
        ident: String,
        seksjonId: String,
        dokumentasjonskrav: String,
    ) {
        sendEttersendingMelding(søknadId, ident, dokumentasjonskrav)
        seksjonRepository.lagreDokumentasjonskravEttersending(søknadId, ident, seksjonId, dokumentasjonskrav)
    }

    fun hentDokumentasjonskrav(
        søknadId: UUID,
        ident: String,
        seksjonId: String,
    ) = seksjonRepository.hentDokumentasjonskrav(søknadId, ident, seksjonId)

    fun sendEttersendingMelding(
        søknadId: UUID,
        ident: String,
        dokumentasjonskrav: String,
    ) {
        val event =
            MeldingOmEttersending(
                søknadId,
                ident,
                opprettDokumenterFraDokumentasjonskravEttersending(
                    dokumentasjonskrav,
                ),
            )
        rapidsConnection.publish(
            ident,
            event.asMessage().toJson(),
        )
    }

    fun opprettDokumenterFraDokumentasjonskravEttersending(dokumentasjonskrav: String): List<Dokument> =
        jsonMapper
            .readTree(dokumentasjonskrav)
            .toList()
            .mapNotNull { rootNode ->
                rootNode
                    .findValue("bundle")
                    ?.let { bundleNode ->
                        if (!bundleNode.isEmpty) {
                            Dokument(
                                rootNode.at("/skjemakode").textValue(),
                                listOf(
                                    Dokumentvariant(
                                        filnavn = bundleNode.at("/filnavn").textValue(),
                                        urn = bundleNode.at("/urn").textValue(),
                                        variant = "ARKIV",
                                        type = "PDF",
                                    ),
                                ),
                            )
                        } else {
                            null
                        }
                    }
            }
}

data class Seksjon(
    val seksjonId: String,
    val data: String,
)
