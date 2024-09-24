package no.nav.dagpenger.soknad.orkestrator.søknad

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import mu.KotlinLogging
import no.nav.dagpenger.soknad.orkestrator.api.models.SporsmaalgruppeNavnDTO
import no.nav.dagpenger.soknad.orkestrator.api.models.SporsmalgruppeDTO
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import no.nav.dagpenger.soknad.orkestrator.metrikker.SøknadMetrikker
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysningstype
import no.nav.dagpenger.soknad.orkestrator.opplysning.Svar
import no.nav.dagpenger.soknad.orkestrator.opplysning.grupper.Bostedsland
import no.nav.dagpenger.soknad.orkestrator.opplysning.grupper.Seksjon
import no.nav.dagpenger.soknad.orkestrator.opplysning.grupper.Seksjonsnavn
import no.nav.dagpenger.soknad.orkestrator.opplysning.grupper.getSeksjon
import no.nav.dagpenger.soknad.orkestrator.opplysning.toSporsmalDTO
import no.nav.dagpenger.soknad.orkestrator.søknad.db.InMemorySøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.db.Spørsmål
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import java.util.UUID

class SøknadService(
    private val søknadRepository: SøknadRepository,
    private val inMemorySøknadRepository: InMemorySøknadRepository = InMemorySøknadRepository(),
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

        val spørsmål =
            Spørsmål(
                spørsmålId = UUID.randomUUID(),
                gruppenavn = Bostedsland.navn,
                gruppespørsmålId = Bostedsland.førsteOpplysningsbehov().id,
                type = Bostedsland.førsteOpplysningsbehov().type,
                svar = null,
            )

        inMemorySøknadRepository.lagre(
            søknadId = søknad.søknadId,
            spørsmål = spørsmål,
        )

        logger.info { "Opprettet søknad med søknadId: ${søknad.søknadId}" }
        sikkerlogg.info { "Opprettet søknad med søknadId: ${søknad.søknadId} og ident: $ident" }

        return søknad
    }

    fun håndterSvar(
        søknadId: UUID,
        svar: Svar<*>,
    ) {
        val (gruppespørsmålId, gruppenavn) = inMemorySøknadRepository.hentGruppeinfo(søknadId, svar.opplysningId)
        val spørsmålgruppe = getSeksjon(gruppenavn!!)

        spørsmålgruppe.validerSvar(gruppespørsmålId!!, svar)
        inMemorySøknadRepository.lagreSvar(søknadId, svar)

        nullstillAvhengigheter(
            søknadId = søknadId,
            gruppe = spørsmålgruppe,
            idIGruppe = gruppespørsmålId,
        )

        håndterNesteSpørsmål(søknadId, svar, gruppenavn, gruppespørsmålId)
    }

    private fun håndterNesteSpørsmål(
        søknadId: UUID,
        svar: Svar<*>,
        gruppenavn: Seksjonsnavn,
        gruppespørsmålId: Int,
    ) {
        val gruppe = getSeksjon(gruppenavn)
        gruppe.nesteOpplysningsbehov(svar, gruppespørsmålId)?.let { nesteSpørsmål ->
            val erLagretIDB =
                inMemorySøknadRepository.hent(
                    søknadId = søknadId,
                    gruppenavn = gruppe.navn,
                    gruppespørsmålId = nesteSpørsmål.id,
                ) != null

            if (!erLagretIDB) {
                val nyttUbesvartSpørsmål =
                    Spørsmål(
                        spørsmålId = UUID.randomUUID(),
                        gruppenavn = gruppe.navn,
                        gruppespørsmålId = nesteSpørsmål.id,
                        type = nesteSpørsmål.type,
                        svar = null,
                    )
                inMemorySøknadRepository.lagre(
                    søknadId = søknadId,
                    spørsmål = nyttUbesvartSpørsmål,
                )
            }
        }
    }

    internal fun slett(
        søknadId: UUID,
        ident: String,
    ) {
        søknadRepository.slett(søknadId)
        inMemorySøknadRepository.slettSøknad(søknadId)

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
        gruppe: Seksjon,
        idIGruppe: Int,
    ) {
        val avhengigheter = gruppe.avhengigheter(idIGruppe)

        avhengigheter.forEach {
            inMemorySøknadRepository.slettSpørsmål(
                søknadId = søknadId,
                gruppenavn = gruppe.navn,
                gruppespørsmålId = it,
            )
        }
    }

    fun nesteSpørsmålgruppe(søknadId: UUID): SporsmalgruppeDTO {
        val alleSpørsmål = inMemorySøknadRepository.hentAlle(søknadId).sortedBy { it.gruppespørsmålId }
        val nesteUbesvartSpørsmål = alleSpørsmål.find { it.svar == null }
        val besvarteSpørsmål =
            if (nesteUbesvartSpørsmål == null) {
                alleSpørsmål
            } else {
                alleSpørsmål.filter { it.gruppespørsmålId < nesteUbesvartSpørsmål.gruppespørsmålId }
            }
        val gruppe = getSeksjon(nesteUbesvartSpørsmål?.gruppenavn ?: Bostedsland.navn) // TODO: Teit med default

        val nesteSpørsmålDTO =
            nesteUbesvartSpørsmål?.let {
                gruppe.getOpplysningsbehov(it.gruppespørsmålId).toSporsmalDTO(it.spørsmålId, null)
            }
        val besvarteSpørsmålDTO =
            besvarteSpørsmål.map {
                gruppe.getOpplysningsbehov(it.gruppespørsmålId).toSporsmalDTO(it.spørsmålId, toJson(it.svar!!))
            }

        return SporsmalgruppeDTO(
            navn = SporsmaalgruppeNavnDTO.valueOf(gruppe.navn.name.lowercase()),
            besvarteSpørsmål = besvarteSpørsmålDTO,
            erFullført = nesteUbesvartSpørsmål == null,
            nesteSpørsmål = nesteSpørsmålDTO,
        )
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
        private val sikkerlogg = KotlinLogging.logger("tjenestekall.SøknadService")
    }
}
