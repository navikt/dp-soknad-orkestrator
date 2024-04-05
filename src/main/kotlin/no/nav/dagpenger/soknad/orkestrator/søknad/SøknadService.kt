package no.nav.dagpenger.soknad.orkestrator.søknad

import mu.KotlinLogging
import no.nav.helse.rapids_rivers.RapidsConnection

class SøknadService(private val rapid: RapidsConnection) {
    fun publiserMeldingOmSøknadInnsendt(søknad: Søknad) {
        rapid.publish(MeldingOmSøknadInnsendt(søknad.id, søknad.ident).asMessage().toJson())

        logger.info { "Publiserte melding om ny søknad med søknadId: ${søknad.id}" }
        sikkerlogg.info { "Publiserte melding om ny søknad med søknadId: ${søknad.id} og ident: ${søknad.ident}" }
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
        private val sikkerlogg = KotlinLogging.logger("tjenestekall.SøknadService")
    }
}
