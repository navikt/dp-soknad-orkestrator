package no.nav.dagpenger.soknad.orkestrator.søknad

import mu.KotlinLogging
import no.nav.dagpenger.soknad.orkestrator.metrikker.SøknadMetrikker
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.helse.rapids_rivers.RapidsConnection
import java.util.UUID

class SøknadService(
    private val rapid: RapidsConnection,
    private val søknadRepository: SøknadRepository,
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

        logger.info { "Opprettet søknad med søknadId: ${søknad.søknadId}" }
        sikkerlogg.info { "Opprettet søknad med søknadId: ${søknad.søknadId} og ident: $ident" }

        return søknad
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
        private val sikkerlogg = KotlinLogging.logger("tjenestekall.SøknadService")
    }
}
