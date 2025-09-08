package no.nav.dagpenger.soknad.orkestrator.personalia

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpHeaders.Authorization
import no.nav.dagpenger.pdl.PersonOppslag
import no.nav.dagpenger.pdl.adresse.AdresseVisitor

class PersonService(
    private val personOppslag: PersonOppslag,
    private val tokenProvider: (subjectToken: String) -> String,
) {
    private val sikkerLogg = KotlinLogging.logger("tjenestekall")

    suspend fun hentPerson(
        fnr: String,
        subjectToken: String,
    ): PersonDto {
        try {
            val person =
                personOppslag.hentPerson(
                    fnr,
                    mapOf(
                        Authorization to "Bearer ${tokenProvider.invoke(subjectToken)}",
                        "behandlingsnummer" to "B286",
                    ),
                )

            val adresseMapper = AdresseMapper(AdresseVisitor(person).adresser)

            return PersonDto(
                fornavn = person.fornavn,
                mellomnavn = person.mellomnavn ?: "",
                etternavn = person.etternavn,
                fodselsDato = person.fodselsdato,
                ident = person.fodselnummer,
                postAdresse = adresseMapper.postAdresse,
                folkeregistrertAdresse = adresseMapper.folkeregistertAdresse,
            )
        } catch (e: Exception) {
            sikkerLogg.error(e) { "Feil under utehenting av person med fnr $fnr fra PDL." }
            throw e
        }
    }
}
