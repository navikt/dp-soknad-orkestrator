package no.nav.dagpenger.soknad.orkestrator.søknad

import mu.KotlinLogging
import no.nav.dagpenger.soknad.orkestrator.api.models.SporsmaalgruppeNavnDTO
import no.nav.dagpenger.soknad.orkestrator.api.models.SporsmalDTO
import no.nav.dagpenger.soknad.orkestrator.api.models.SporsmalgruppeDTO
import no.nav.dagpenger.soknad.orkestrator.metrikker.SøknadMetrikker
import no.nav.dagpenger.soknad.orkestrator.spørsmål.grupper.Bostedsland
import no.nav.dagpenger.soknad.orkestrator.spørsmål.grupper.BostedslandDTOV1
import no.nav.dagpenger.soknad.orkestrator.spørsmål.grupper.getGruppe
import no.nav.dagpenger.soknad.orkestrator.spørsmål.grupper.toSporsmalDTO
import no.nav.dagpenger.soknad.orkestrator.søknad.db.InMemorySøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.helse.rapids_rivers.RapidsConnection
import java.util.UUID

class SøknadService(
    private val rapid: RapidsConnection,
    private val søknadRepository: SøknadRepository,
    private val inMemorySøknadRepository: InMemorySøknadRepository = InMemorySøknadRepository(),
) {
    fun publiserMeldingOmSøknadInnsendt(
        søknadId: UUID,
        ident: String,
    ) {
        rapid.publish(MeldingOmSøknadInnsendt(søknadId, ident).asMessage().toJson())
        SøknadMetrikker.varslet.inc()

        logger.info { "Publiserte melding om ny søknad med søknadId: $søknadId" }
        sikkerlogg.info { "Publiserte melding om ny søknad med søknadId: $søknadId og ident: $ident" }
    }

    fun opprettSøknad(ident: String): Søknad {
        val søknad = Søknad(ident = ident)
        søknadRepository.lagre(søknad)

        val bostedsland = BostedslandDTOV1
        inMemorySøknadRepository.lagre(
            spørsmålId = UUID.randomUUID(),
            søknadId = søknad.søknadId,
            gruppeId = bostedsland.versjon,
            idIGruppe = bostedsland.førsteSpørsmål().idIGruppe,
            svar = null,
        )

        logger.info { "Opprettet søknad med søknadId: ${søknad.søknadId}" }
        sikkerlogg.info { "Opprettet søknad med søknadId: ${søknad.søknadId} og ident: $ident" }

        return søknad
    }

    fun lagreBesvartSpørsmål(
        søknadId: UUID,
        besvartSpørsmål: SporsmalDTO,
    ) {
        val lagretInfo =
            inMemorySøknadRepository.hent(
                søknadId = søknadId,
                spørsmålId = besvartSpørsmål.id,
            ) ?: throw IllegalArgumentException("Fant ikke spørsmål med id ${besvartSpørsmål.id}")

        inMemorySøknadRepository.lagre(
            spørsmålId = besvartSpørsmål.id,
            søknadId = søknadId,
            gruppeId = lagretInfo.gruppeId,
            idIGruppe = lagretInfo.idIGruppe,
            svar = besvartSpørsmål.svar,
        )

        val gruppe = getGruppe(lagretInfo.gruppeId)

        nullstillAvhengigheter(
            søknadId = søknadId,
            gruppe = gruppe,
            idIGruppe = lagretInfo.idIGruppe,
        )

        gruppe.nesteSpørsmål(besvartSpørsmål)?.let {
            val nesteSpørsmålFinnesAllerede =
                inMemorySøknadRepository.hent(
                    søknadId = søknadId,
                    gruppeId = gruppe.versjon,
                    spørsmålIdIGruppe = it.idIGruppe,
                ) != null

            if (!nesteSpørsmålFinnesAllerede) {
                inMemorySøknadRepository.lagre(
                    spørsmålId = UUID.randomUUID(),
                    søknadId = søknadId,
                    gruppeId = lagretInfo.gruppeId,
                    idIGruppe = it.idIGruppe,
                    svar = null,
                )
            }
        }
    }

    fun nullstillAvhengigheter(
        søknadId: UUID,
        gruppe: Bostedsland,
        idIGruppe: Int,
    ) {
        val avhengigheter = gruppe.avhengigheter(idIGruppe)

        avhengigheter.forEach {
            inMemorySøknadRepository.slett(
                søknadId = søknadId,
                gruppeId = gruppe.versjon,
                spørsmålIdIGruppe = it,
            )
        }
    }

    fun nesteSpørsmålgruppe(søknadId: UUID): SporsmalgruppeDTO {
        val alleSpørsmål = inMemorySøknadRepository.hentAlle(søknadId).sortedBy { it.idIGruppe }
        val førsteUbesvartSpørsmål = alleSpørsmål.find { it.svar == null }
        val besvarteSpørsmål =
            if (førsteUbesvartSpørsmål == null) alleSpørsmål else alleSpørsmål.filter { it.idIGruppe < førsteUbesvartSpørsmål.idIGruppe }
        val gruppe = getGruppe(førsteUbesvartSpørsmål?.gruppeId ?: BostedslandDTOV1.versjon)

        val nesteSpørsmålDTO =
            førsteUbesvartSpørsmål?.let {
                gruppe.getSpørsmålMedId(it.idIGruppe).toSporsmalDTO(it.spørsmålId, null)
            }
        val besvarteSpørsmålDTO =
            besvarteSpørsmål.map {
                gruppe.getSpørsmålMedId(it.idIGruppe).toSporsmalDTO(it.spørsmålId, it.svar!!)
            }

        return SporsmalgruppeDTO(
            id = gruppe.versjon,
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
