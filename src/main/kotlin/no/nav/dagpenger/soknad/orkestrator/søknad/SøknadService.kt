package no.nav.dagpenger.soknad.orkestrator.søknad

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import mu.KotlinLogging
import no.nav.dagpenger.soknad.orkestrator.api.models.SeksjonDTO
import no.nav.dagpenger.soknad.orkestrator.api.models.SeksjonsnavnDTO
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import no.nav.dagpenger.soknad.orkestrator.metrikker.SøknadMetrikker
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysning
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysningstype
import no.nav.dagpenger.soknad.orkestrator.opplysning.Svar
import no.nav.dagpenger.soknad.orkestrator.opplysning.db.OpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.opplysning.seksjoner.Bostedsland
import no.nav.dagpenger.soknad.orkestrator.opplysning.seksjoner.Seksjon
import no.nav.dagpenger.soknad.orkestrator.opplysning.seksjoner.getSeksjon
import no.nav.dagpenger.soknad.orkestrator.opplysning.toOpplysningDTO
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import java.util.UUID

class SøknadService(
    private val søknadRepository: SøknadRepository,
    private val opplysningRepository: OpplysningRepository,
) {
    private lateinit var rapidsConnection: RapidsConnection

    fun setRapidsConnection(rapidsConnection: RapidsConnection) {
        this.rapidsConnection = rapidsConnection
    }

    fun søknadFinnes(søknadId: UUID) = søknadRepository.hent(søknadId) != null

    fun publiserMeldingOmSøknadInnsendt(
        søknadId: UUID,
        ident: String,
    ) {
        rapidsConnection.publish(ident, MeldingOmSøknadInnsendt(søknadId, ident).asMessage().toJson())
        SøknadMetrikker.varslet.inc()

        logger.info { "Publiserte melding om ny søknad med søknadId: $søknadId" }
        sikkerlogg.info { "Publiserte melding om ny søknad med søknadId: $søknadId og ident: $ident" }
    }

    fun hentEllerOpprettSøknad(ident: String): Søknad {
        søknadRepository.hentPåbegynt(ident)?.let {
            logger.info { "Fant påbegynt søknad med id: ${it.søknadId}. Oppretter ikke ny." }
            sikkerlogg.info { "Fant påbegynt søknad med id: ${it.søknadId} og ident: $ident. Oppretter ikke ny." }
            return it
        }

        val nySøknad = Søknad(ident = ident).also { søknadRepository.lagre(it) }
        val seksjon = getSeksjon(Bostedsland.navn)
        opplysningRepository.opprettSeksjon(søknadId = nySøknad.søknadId, seksjon = seksjon)

        Opplysning(
            opplysningId = UUID.randomUUID(),
            seksjonsnavn = seksjon.navn,
            opplysningsbehovId = seksjon.førsteOpplysningsbehov().id,
            type = seksjon.førsteOpplysningsbehov().type,
            svar = null,
        ).also { opplysningRepository.lagre(nySøknad.søknadId, it) }

        logger.info { "Opprettet søknad med søknadId: ${nySøknad.søknadId}" }
        sikkerlogg.info { "Opprettet søknad med søknadId: ${nySøknad.søknadId} og ident: $ident" }

        return nySøknad
    }

    fun håndterSvar(
        søknadId: UUID,
        svar: Svar<*>,
    ) {
        val opplysning =
            opplysningRepository.hent(svar.opplysningId)
                ?: throw IllegalArgumentException("Fant ikke opplysning med id: ${svar.opplysningId}, kan ikke håndtere svar")

        val opplysningsbehovId = opplysning.opplysningsbehovId
        val seksjonsnavn = opplysning.seksjonsnavn
        val seksjon = getSeksjon(seksjonsnavn)

        try {
            seksjon.validerSvar(opplysningsbehovId, svar)
            opplysningRepository.lagreSvar(svar)
        } catch (e: IllegalArgumentException) {
            logger.error(e) { "Validering av svar feilet" }
            return
        }

        nullstillAvhengigheter(
            søknadId = søknadId,
            seksjon = seksjon,
            opplysningsbehovId = opplysningsbehovId,
        )

        håndterNesteOpplysning(søknadId, svar, seksjon, opplysningsbehovId)
    }

    private fun håndterNesteOpplysning(
        søknadId: UUID,
        svar: Svar<*>,
        seksjon: Seksjon,
        opplysningsbehovId: Int,
    ) {
        seksjon.nesteOpplysningsbehov(svar, opplysningsbehovId)?.let { nesteOpplysning ->
            val erLagretIDB =
                opplysningRepository
                    .hentAlleForSeksjon(søknadId, seksjon.navn)
                    .find { it.opplysningsbehovId == nesteOpplysning.id } != null

            if (!erLagretIDB) {
                val nyOpplysning =
                    Opplysning(
                        opplysningId = UUID.randomUUID(),
                        seksjonsnavn = seksjon.navn,
                        opplysningsbehovId = nesteOpplysning.id,
                        type = nesteOpplysning.type,
                        svar = null,
                    )

                opplysningRepository.lagre(søknadId, nyOpplysning)
            }
        }
    }

    internal fun slett(
        søknadId: UUID,
        ident: String,
    ) {
        søknadRepository.slett(søknadId)

        SøknadMetrikker.slettet.inc()
        logger.info { "Slettet søknad med søknadId: $søknadId" }
        sikkerlogg.info { "Slettet søknad med søknadId: $søknadId og ident: $ident" }
    }

    private fun toJson(svar: Svar<*>): String? =
        when (svar.type) {
            Opplysningstype.LAND, Opplysningstype.TEKST -> svar.verdi.toString()
            else -> objectMapper.writeValueAsString(svar.verdi)
        }

    private fun nullstillAvhengigheter(
        søknadId: UUID,
        seksjon: Seksjon,
        opplysningsbehovId: Int,
    ) {
        val avhengigheter = seksjon.avhengigheter(opplysningsbehovId)

        avhengigheter.forEach {
            opplysningRepository.slett(
                søknadId = søknadId,
                seksjonsnavn = seksjon.navn,
                opplysningsbehovId = it,
            )
        }
    }

    fun nesteSeksjon(søknadId: UUID): List<SeksjonDTO> {
        val alleOpplysninger = opplysningRepository.hentAlle(søknadId)
        val ubesvarteOpplysninger = alleOpplysninger.filter { it.svar == null }.sortedBy { it.opplysningsbehovId }
        val nesteUbesvarteOpplysning = ubesvarteOpplysninger.firstOrNull()

        val besvarteOpplysninger =
            if (nesteUbesvarteOpplysning == null) {
                alleOpplysninger
            } else {
                alleOpplysninger.filter { it.svar != null && it.opplysningsbehovId < nesteUbesvarteOpplysning.opplysningsbehovId }
            }
        val seksjon = getSeksjon(Bostedsland.navn) // TODO: Teit med default

        val nesteUbesvarteOpplysningDTO =
            nesteUbesvarteOpplysning?.let {
                seksjon.getOpplysningsbehov(it.opplysningsbehovId).toOpplysningDTO(it.opplysningId, null)
            }
        val besvarteOpplysningerDTO =
            besvarteOpplysninger.map {
                seksjon.getOpplysningsbehov(it.opplysningsbehovId).toOpplysningDTO(it.opplysningId, toJson(it.svar!!))
            }

        return listOf(
            SeksjonDTO(
                navn = SeksjonsnavnDTO.valueOf(seksjon.navn.name.lowercase()),
                besvarteOpplysninger = besvarteOpplysningerDTO,
                erFullført = nesteUbesvarteOpplysning == null,
                nesteUbesvarteOpplysning = nesteUbesvarteOpplysningDTO,
            ),
        )
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
        private val sikkerlogg = KotlinLogging.logger("tjenestekall.SøknadService")
    }
}
