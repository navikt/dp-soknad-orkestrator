package no.nav.dagpenger.soknad.orkestrator.søknad.seksjon

import com.fasterxml.jackson.databind.json.JsonMapper
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.soknad.orkestrator.søknad.Dokument
import no.nav.dagpenger.soknad.orkestrator.søknad.Dokumentvariant
import no.nav.dagpenger.soknad.orkestrator.søknad.SøknadService
import no.nav.dagpenger.soknad.orkestrator.søknad.Tilstand
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.melding.MeldingOmEttersending
import no.nav.dagpenger.soknad.orkestrator.søknad.melding.SeksjonDataTilStatistikk
import java.time.LocalDateTime
import java.util.UUID

class SeksjonService(
    val seksjonRepository: SeksjonRepository,
    val søknadRepository: SøknadRepository,
    val søknadService: SøknadService,
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
        logg.info { "Lagrer seksjon $seksjonId" }
        sikkerlogg.info { "Lagrer seksjon $seksjonId, for ident: $ident, med svar: $seksjonsvar, dokumentasjonskrav: $dokumentasjonskrav" }
        seksjonRepository.lagre(søknadId, ident, seksjonId, seksjonsvar, dokumentasjonskrav, pdfGrunnlag)

        val seksjon = seksjonRepository.hentSeksjonMetadata(søknadId, ident, seksjonId)

        val sendPåbegyntSøknadTilStatistikk =
            SeksjonDataTilStatistikk(
                søknadId = søknadId,
                ident = ident,
                seksjonId = seksjonId,
                opprettet = seksjon.opprettet,
                oppdatert = seksjon.oppdatert ?: seksjon.opprettet,
            )

        rapidsConnection.publish(
            ident,
            sendPåbegyntSøknadTilStatistikk.asMessage().toJson(),
        )
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
        søknadRepository.verifiserAtSøknadEksistererOgTilhørerIdent(søknadId, ident)
        søknadRepository.verifiserAtSøknadenHarEnAvTilstandene(
            søknadId = søknadId,
            forventedeTilstander = listOf(Tilstand.INNSENDT, Tilstand.JOURNALFØRT),
        )
        sendEttersendingMelding(søknadId, ident, seksjonId, dokumentasjonskrav)
    }

    fun hentDokumentasjonskrav(
        søknadId: UUID,
        ident: String,
        seksjonId: String,
    ) = seksjonRepository.hentDokumentasjonskrav(søknadId, ident, seksjonId)

    private fun sendEttersendingMelding(
        søknadId: UUID,
        ident: String,
        seksjonId: String,
        dokumentasjonskrav: String,
    ) {
        val hoveddokumentSkjemakode = søknadService.finnSkjemaKode(ident, søknadId, forventetFullførtSøknad = false)
        logg.info { "Sender ettersending for søknadId=$søknadId, hoveddokumentSkjemakode=$hoveddokumentSkjemakode" }
        val event =
            MeldingOmEttersending(
                søknadId = søknadId,
                ident = ident,
                dokumentKravene =
                    opprettDokumenterFraDokumentasjonskravEttersending(
                        dokumentasjonskrav = dokumentasjonskrav,
                        hoveddokumentSkjemakode = hoveddokumentSkjemakode,
                    ),
                dokumentasjonskravJson = dokumentasjonskrav,
                seksjonId = seksjonId,
            )
        rapidsConnection.publish(
            ident,
            event.asMessage().toJson(),
        )
    }

    fun opprettDokumenterFraDokumentasjonskravEttersending(
        dokumentasjonskrav: String,
        hoveddokumentSkjemakode: String? = null,
    ): List<Dokument> {
        val dokumenter =
            jsonMapper
                .readTree(dokumentasjonskrav)
                .toList()
                .mapNotNull { rootNode ->
                    rootNode
                        .findValue("bundle")
                        ?.let { bundleNode ->
                            if (!bundleNode.isEmpty) {
                                Dokument(
                                    skjemakode = rootNode.at("/skjemakode").textValue(),
                                    varianter =
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
        if (hoveddokumentSkjemakode != null && dokumenter.isNotEmpty()) {
            return listOf(dokumenter.first().copy(skjemakode = hoveddokumentSkjemakode)) + dokumenter.drop(1)
        }
        return dokumenter
    }
}

data class Seksjon(
    val seksjonId: String,
    val data: String,
)

data class SeksjonMedTidstempler(
    val seksjonId: String,
    val data: String,
    val opprettet: LocalDateTime,
    val oppdatert: LocalDateTime?,
)

data class SeksjonMetadata(
    val seksjonId: String,
    val opprettet: LocalDateTime,
    val oppdatert: LocalDateTime?,
)
