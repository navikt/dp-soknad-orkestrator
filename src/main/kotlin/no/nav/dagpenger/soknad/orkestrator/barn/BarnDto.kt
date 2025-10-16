package no.nav.dagpenger.soknad.orkestrator.barn

import java.time.LocalDate

data class BarnDto(
    val fornavn: String = "",
    val mellomnavn: String = "",
    val fornavnOgMellomnavn: String = listOf(fornavn, mellomnavn).filterNot(String?::isNullOrBlank).joinToString(" "),
    val etternavn: String = "",
    val fødselsdato: LocalDate,
    val alder: Long = 0,
    val bostedsland: String = "",
)
