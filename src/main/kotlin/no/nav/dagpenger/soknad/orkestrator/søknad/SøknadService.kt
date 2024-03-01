package no.nav.dagpenger.soknad.orkestrator.søknad

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class SøknadService {
    fun håndter(legacySøknad: LegacySøknad) {
        toSøknad(legacySøknad)
    }
}

fun toSøknad(legacySøknad: LegacySøknad): Søknad {
    val opplysninger =
        legacySøknad.søknadsData.seksjoner.map { seksjon ->
            seksjon.fakta.map { fakta ->
                Opplysning(fakta.svar, fakta.beskrivendeId)
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

fun String.toLocalDateTime(): LocalDateTime {
    return LocalDateTime.parse(this, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS"))
}
