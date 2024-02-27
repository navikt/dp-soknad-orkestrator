package no.nav.dagpenger.soknad.orkestrator.søknadmottak

import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysning
import no.nav.dagpenger.soknad.orkestrator.opplysning.Søknad
import java.util.UUID

class SøknadMapper(legacySøknad: LegacySøknad) {
    private val opplysninger =
        legacySøknad.søknadsData.seksjoner.map { seksjon ->
            seksjon.fakta.map { fakta ->
                Opplysning(fakta.svar, fakta.beskrivendeId)
            }
        }.toList().flatten()

    val søknad by lazy {
        Søknad(
            id = UUID.fromString(legacySøknad.søknadsData.søknad_uuid),
            fødselsnummer = legacySøknad.fødselsnummer,
            journalpostId = legacySøknad.journalpostId,
            // TODO: Finne nøyaktig søknadstidspunkt
            søknadstidspunkt = legacySøknad.opprettet.toLocalDateTime(),
            opplysninger = opplysninger,
        )
    }
}
