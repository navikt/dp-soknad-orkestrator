package no.nav.dagpenger.soknad.orkestrator.søknadmottak

import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysning
import no.nav.dagpenger.soknad.orkestrator.opplysning.Søknad
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class SøknadMapper() {
    fun toSøknad(legacySøknad: LegacySøknad): Søknad {
        val opplysninger =
            legacySøknad.søknadsData.seksjoner.map { seksjon ->
                seksjon.fakta.map { fakta ->
                    Opplysning(fakta.svar, fakta.beskrivendeId)
                }
            }.toList().flatten()

        return Søknad(
            id = UUID.fromString(legacySøknad.søknadsData.søknad_uuid),
            fødselsnummer = legacySøknad.fødselsnummer,
            journalpostId = legacySøknad.journalpostId,
            // TODO: Finne nøyaktig søknadstidspunkt
            søknadstidspunkt = legacySøknad.opprettet.toLocalDateTime(),
            opplysninger = opplysninger,
        )
    }
}

fun String.toLocalDateTime(): LocalDateTime {
    return LocalDateTime.parse(this, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS"))
}
