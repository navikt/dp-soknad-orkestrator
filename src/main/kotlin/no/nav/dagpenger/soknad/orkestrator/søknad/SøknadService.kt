package no.nav.dagpenger.soknad.orkestrator.søknad

import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysning
import no.nav.helse.rapids_rivers.RapidsConnection

class SøknadService(private val rapid: RapidsConnection) {
    fun mapTilSøknad(legacySøknad: LegacySøknad) = toSøknad(legacySøknad)

    fun publiserMeldingOmNySøknad(søknad: Søknad) {
        rapid.publish(MeldingOmNySøknad(søknad.id, søknad.fødselsnummer).asMessage().toJson())
    }
}

fun toSøknad(legacySøknad: LegacySøknad): Søknad {
    val opplysninger =
        legacySøknad.søknadsData.seksjoner.map { seksjon ->
            seksjon.fakta.map { fakta ->
                Opplysning(
                    svar = fakta.svar,
                    beskrivendeId = fakta.beskrivendeId,
                    søknadsId = legacySøknad.søknadsData.søknadUUID,
                    fødselsnummer = legacySøknad.fødselsnummer,
                )
            }
        }.toList().flatten()

    return Søknad(
        id = legacySøknad.søknadsData.søknadUUID,
        fødselsnummer = legacySøknad.fødselsnummer,
        journalpostId = legacySøknad.journalpostId,
        // TODO: Finne nøyaktig søknadstidspunkt
        søknadstidspunkt = legacySøknad.opprettet,
        opplysninger = opplysninger,
    )
}
