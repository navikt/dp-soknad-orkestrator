package no.nav.dagpenger.soknad.orkestrator.søknad

import no.nav.helse.rapids_rivers.RapidsConnection

class SøknadService(private val rapid: RapidsConnection) {
    fun publiserMeldingOmNySøknad(søknad: Søknad) {
        rapid.publish(MeldingOmNySøknad(søknad.id, søknad.ident).asMessage().toJson())
    }
}
