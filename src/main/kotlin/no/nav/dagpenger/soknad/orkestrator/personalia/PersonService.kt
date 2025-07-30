package no.nav.dagpenger.soknad.orkestrator.personalia

import io.ktor.http.HttpHeaders.Authorization
import no.nav.dagpenger.pdl.PersonOppslag
import no.nav.dagpenger.pdl.adresse.AdresseVisitor

class PersonService(
    private val personOppslag: PersonOppslag,
    private val tokenProvider: (subjectToken: String) -> String,
) {
    suspend fun hentPerson(
        fnr: String,
        subjectToken: String,
    ): PersonDto {
        val person =
            personOppslag.hentPerson(
                fnr,
                mapOf(
                    Authorization to "Bearer ${tokenProvider.invoke(subjectToken)}",
                    // TODO: Skal denne endres (kopiert fra dp-soknad)?
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
    }
}
