package no.nav.dagpenger.soknad.orkestrator.søknad

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import mu.KotlinLogging
import no.nav.dagpenger.soknad.orkestrator.api.models.SporsmaalgruppeNavnDTO
import no.nav.dagpenger.soknad.orkestrator.api.models.SporsmalgruppeDTO
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import no.nav.dagpenger.soknad.orkestrator.metrikker.SøknadMetrikker
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysning
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysningstype
import no.nav.dagpenger.soknad.orkestrator.opplysning.Svar
import no.nav.dagpenger.soknad.orkestrator.opplysning.db.OpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.opplysning.grupper.Bostedsland
import no.nav.dagpenger.soknad.orkestrator.opplysning.grupper.Seksjon
import no.nav.dagpenger.soknad.orkestrator.opplysning.grupper.getSeksjon
import no.nav.dagpenger.soknad.orkestrator.opplysning.toSporsmalDTO
import no.nav.dagpenger.soknad.orkestrator.søknad.db.InMemorySøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import java.util.UUID

class SøknadService(
    private val søknadRepository: SøknadRepository,
    private val inMemorySøknadRepository: InMemorySøknadRepository = InMemorySøknadRepository(),
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

    fun opprettSøknad(ident: String): Søknad {
        val søknad = Søknad(ident = ident)
        søknadRepository.lagre(søknad)

        val opplysning =
            Opplysning(
                opplysningId = UUID.randomUUID(),
                seksjonversjon = Bostedsland.versjon,
                opplysningsbehovId = Bostedsland.førsteOpplysningsbehov().id,
                type = Bostedsland.førsteOpplysningsbehov().type,
                svar = null,
            )

        opplysningRepository.opprettSeksjon(søknadId = søknad.søknadId, versjon = opplysning.seksjonversjon)

        opplysningRepository.lagre(
            søknadId = søknad.søknadId,
            opplysning = opplysning,
        )

        logger.info { "Opprettet søknad med søknadId: ${søknad.søknadId}" }
        sikkerlogg.info { "Opprettet søknad med søknadId: ${søknad.søknadId} og ident: $ident" }

        return søknad
    }

    fun håndterSvar(
        søknadId: UUID,
        svar: Svar<*>,
    ) {
        val opplysning =
            opplysningRepository.hent(svar.opplysningId)
                ?: throw IllegalArgumentException("Fant ikke opplysning med id: ${svar.opplysningId}, kan ikke håndtere svar")

        val opplysningsbehovId = opplysning.opplysningsbehovId
        val seksjonversjon = opplysning.seksjonversjon
        val seksjon = getSeksjon(seksjonversjon)

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

        håndterNesteSpørsmål(søknadId, svar, seksjon, opplysningsbehovId)
    }

    private fun håndterNesteSpørsmål(
        søknadId: UUID,
        svar: Svar<*>,
        seksjon: Seksjon,
        opplysningsbehovId: Int,
    ) {
        seksjon.nesteOpplysningsbehov(svar, opplysningsbehovId)?.let { nesteOpplysning ->
            val erLagretIDB =
                opplysningRepository.hentAlleForSeksjon(søknadId, seksjon.versjon)
                    .find { it.opplysningsbehovId == nesteOpplysning.id } != null

            if (!erLagretIDB) {
                val nyOpplysning =
                    Opplysning(
                        opplysningId = UUID.randomUUID(),
                        seksjonversjon = seksjon.versjon,
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
                seksjonversjon = seksjon.versjon,
                opplysningsbehovId = it,
            )
        }
    }

    fun nesteSeksjon(søknadId: UUID): SporsmalgruppeDTO {
        val alleOpplysninger = opplysningRepository.hentAlle(søknadId)
        val ubesvarteOpplysninger = alleOpplysninger.filter { it.svar == null }.sortedBy { it.opplysningsbehovId }
        val nesteUbesvarteOpplysning = ubesvarteOpplysninger.firstOrNull()

        val besvarteOpplysninger =
            if (nesteUbesvarteOpplysning == null) {
                alleOpplysninger
            } else {
                alleOpplysninger.filter { it.svar != null && it.opplysningsbehovId < nesteUbesvarteOpplysning.opplysningsbehovId }
            }
        val seksjon = getSeksjon(Bostedsland.versjon) // TODO: Teit med default

        val nesteUbesvarteOpplysningDTO =
            nesteUbesvarteOpplysning?.let {
                seksjon.getOpplysningsbehov(it.opplysningsbehovId).toSporsmalDTO(it.opplysningId, null)
            }
        val besvarteOpplysningerDTO =
            besvarteOpplysninger.map {
                seksjon.getOpplysningsbehov(it.opplysningsbehovId).toSporsmalDTO(it.opplysningId, toJson(it.svar!!))
            }

        return SporsmalgruppeDTO(
            navn = SporsmaalgruppeNavnDTO.valueOf(seksjon.navn.name.lowercase()),
            besvarteSpørsmål = besvarteOpplysningerDTO,
            erFullført = nesteUbesvarteOpplysning == null,
            nesteSpørsmål = nesteUbesvarteOpplysningDTO,
        )
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
        private val sikkerlogg = KotlinLogging.logger("tjenestekall.SøknadService")
    }
}
