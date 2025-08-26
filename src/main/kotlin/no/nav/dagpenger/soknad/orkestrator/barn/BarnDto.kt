package no.nav.dagpenger.soknad.orkestrator.barn

import java.time.LocalDate
import java.time.LocalDate.now
import java.time.temporal.ChronoUnit.YEARS

data class BarnDto(
    val fornavn: String = "",
    val mellomnavn: String = "",
    val fornavnOgMellomnavn: String = listOf(fornavn, mellomnavn).filterNot(String?::isNullOrBlank).joinToString(" "),
    val etternavn: String = "",
    val fødselsdato: LocalDate,
    val bostedsland: String = "",
) {
    fun alder(): Int = YEARS.between(this.fødselsdato, now()).toInt()
}
