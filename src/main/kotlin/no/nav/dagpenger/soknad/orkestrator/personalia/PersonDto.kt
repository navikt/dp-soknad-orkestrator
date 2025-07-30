package no.nav.dagpenger.soknad.orkestrator.personalia

import java.time.LocalDate

data class PersonDto(
    val fornavn: String = "",
    val mellomnavn: String = "",
    val etternavn: String = "",
    val fodselsDato: LocalDate,
    val ident: String,
    val postAdresse: AdresseDto?,
    val folkeregistrertAdresse: AdresseDto?,
)
