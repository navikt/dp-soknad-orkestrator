package no.nav.dagpenger.soknad.orkestrator.søknad

import mu.KotlinLogging
import no.nav.dagpenger.soknad.orkestrator.api.models.SporsmaalgruppeNavnDTO
import no.nav.dagpenger.soknad.orkestrator.api.models.SporsmalDTO
import no.nav.dagpenger.soknad.orkestrator.api.models.SporsmalgruppeDTO
import no.nav.dagpenger.soknad.orkestrator.metrikker.SøknadMetrikker
import no.nav.dagpenger.soknad.orkestrator.spørsmål.grupper.BostedslandDTO
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

        val bostedsland = BostedslandDTO()
        inMemorySøknadRepository.lagre(søknad.søknadId, bostedsland.hvilketLandBorDuI)

        logger.info { "Opprettet søknad med søknadId: ${søknad.søknadId}" }
        sikkerlogg.info { "Opprettet søknad med søknadId: ${søknad.søknadId} og ident: $ident" }

        return søknad
    }

    fun lagreBesvartSpørsmål(
        søknadId: UUID,
        besvartSpørsmål: SporsmalDTO,
    ) {
        inMemorySøknadRepository.lagre(søknadId, besvartSpørsmål)

        val nesteSpørsmål = BostedslandDTO().nesteSpørsmål(besvartSpørsmål)
        if (nesteSpørsmål != null) {
            inMemorySøknadRepository.lagre(søknadId, nesteSpørsmål)
        }
    }

    fun nesteSpørsmålgruppe(søknadId: UUID): SporsmalgruppeDTO {
        val alleSpørsmål = inMemorySøknadRepository.hentAlle(søknadId)
        val ubesvartSpørsmål = alleSpørsmål.find { it.svar == null }

        return SporsmalgruppeDTO(
            id = 1,
            navn = SporsmaalgruppeNavnDTO.BOSTEDSLAND,
            nesteSpørsmål = ubesvartSpørsmål,
            besvarteSpørsmål = alleSpørsmål.filter { it.svar != null },
        )
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
        private val sikkerlogg = KotlinLogging.logger("tjenestekall.SøknadService")
    }
}
