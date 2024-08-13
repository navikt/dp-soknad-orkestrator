package no.nav.dagpenger.soknad.orkestrator.søknad

import mu.KotlinLogging
import no.nav.dagpenger.soknad.orkestrator.api.models.SporsmaalgruppeNavnDTO
import no.nav.dagpenger.soknad.orkestrator.api.models.SporsmalgruppeDTO
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import no.nav.dagpenger.soknad.orkestrator.metrikker.SøknadMetrikker
import no.nav.dagpenger.soknad.orkestrator.spørsmål.SpørsmålType
import no.nav.dagpenger.soknad.orkestrator.spørsmål.Svar
import no.nav.dagpenger.soknad.orkestrator.spørsmål.grupper.Bostedsland
import no.nav.dagpenger.soknad.orkestrator.spørsmål.grupper.Spørsmålgruppe
import no.nav.dagpenger.soknad.orkestrator.spørsmål.grupper.getSpørsmålgruppe
import no.nav.dagpenger.soknad.orkestrator.spørsmål.grupper.toSporsmalDTO
import no.nav.dagpenger.soknad.orkestrator.søknad.db.InMemorySøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.db.Spørsmål
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.helse.rapids_rivers.RapidsConnection
import java.util.UUID

class SøknadService(
    private val rapid: RapidsConnection,
    private val søknadRepository: SøknadRepository,
    private val inMemorySøknadRepository: InMemorySøknadRepository = InMemorySøknadRepository(),
) {
    fun søknadFinnes(søknadId: UUID) = søknadRepository.hent(søknadId) != null

    fun publiserMeldingOmSøknadInnsendt(
        søknadId: UUID,
        ident: String,
    ) {
        rapid.publish(ident, MeldingOmSøknadInnsendt(søknadId, ident).asMessage().toJson())
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
                gruppespørsmålId = Bostedsland.førsteSpørsmål().id,
                type = Bostedsland.førsteSpørsmål().type,
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
        spørsmålId: UUID,
        svar: Svar<*>,
    ) {
        val spørsmålSomSkalBesvares =
            inMemorySøknadRepository.hent(
                søknadId = søknadId,
                spørsmålId = spørsmålId,
            ) ?: throw IllegalArgumentException("Fant ikke spørsmål med id $spørsmålId")

        val spørsmålgruppe = getSpørsmålgruppe(spørsmålSomSkalBesvares.gruppenavn)
        spørsmålgruppe.validerSvar(spørsmålSomSkalBesvares.gruppespørsmålId, svar)

        val besvartSpørsmål = spørsmålSomSkalBesvares.copy(svar = svar)

        inMemorySøknadRepository.lagre(
            søknadId = søknadId,
            spørsmål = besvartSpørsmål,
        )

        nullstillAvhengigheter(
            søknadId = søknadId,
            gruppe = spørsmålgruppe,
            idIGruppe = besvartSpørsmål.gruppespørsmålId,
        )

        håndterNesteSpørsmål(besvartSpørsmål, søknadId)
    }

    private fun håndterNesteSpørsmål(
        besvartSpørsmål: Spørsmål,
        søknadId: UUID,
    ) {
        val gruppe = getSpørsmålgruppe(besvartSpørsmål.gruppenavn)
        gruppe.nesteSpørsmål(besvartSpørsmål)?.let { nesteSpørsmål ->
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

    private fun toJson(svar: Svar<*>): String? =
        when (svar.type) {
            SpørsmålType.LAND, SpørsmålType.TEKST -> svar.verdi.toString()
            else -> objectMapper.writeValueAsString(svar.verdi)
        }

    private fun nullstillAvhengigheter(
        søknadId: UUID,
        gruppe: Spørsmålgruppe,
        idIGruppe: Int,
    ) {
        val avhengigheter = gruppe.avhengigheter(idIGruppe)

        avhengigheter.forEach {
            inMemorySøknadRepository.slett(
                søknadId = søknadId,
                gruppenavn = gruppe.navn,
                gruppespørsmålId = it,
            )
        }
    }

    fun nesteSpørsmålgruppe(søknadId: UUID): SporsmalgruppeDTO {
        val alleSpørsmål = inMemorySøknadRepository.hentAlle(søknadId).sortedBy { it.gruppespørsmålId }
        val førsteUbesvartSpørsmål = alleSpørsmål.find { it.svar == null }
        val besvarteSpørsmål =
            if (førsteUbesvartSpørsmål ==
                null
            ) {
                alleSpørsmål
            } else {
                alleSpørsmål.filter { it.gruppespørsmålId < førsteUbesvartSpørsmål.gruppespørsmålId }
            }
        val gruppe = getSpørsmålgruppe(førsteUbesvartSpørsmål?.gruppenavn ?: Bostedsland.navn) // TODO: Teit med default

        val nesteSpørsmålDTO =
            førsteUbesvartSpørsmål?.let {
                gruppe.getSpørsmål(it.gruppespørsmålId).toSporsmalDTO(it.spørsmålId, null)
            }
        val besvarteSpørsmålDTO =
            besvarteSpørsmål.map {
                gruppe.getSpørsmål(it.gruppespørsmålId).toSporsmalDTO(it.spørsmålId, toJson(it.svar!!))
            }

        return SporsmalgruppeDTO(
            navn = SporsmaalgruppeNavnDTO.valueOf(gruppe.navn.name),
            besvarteSpørsmål = besvarteSpørsmålDTO,
            nesteSpørsmål = nesteSpørsmålDTO,
        )
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
        private val sikkerlogg = KotlinLogging.logger("tjenestekall.SøknadService")
    }
}
