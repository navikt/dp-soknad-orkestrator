package no.nav.dagpenger.soknad.orkestrator.barn

import java.time.LocalDate
import java.time.LocalDate.now
import java.time.temporal.ChronoUnit.YEARS

data class BarnDto(
    val fornavn: String = "",
    val mellomnavn: String = "",
    val fornavnOgMellomnavn: String = listOf(fornavn, mellomnavn).filterNot(String?::isNullOrBlank).joinToString(" "),
    val etternavn: String = "",
    val fodselsdato: LocalDate,
    val bostedsland: String = "",
    val hentetFraPdl: Boolean,
) {
    fun alder(): Int = YEARS.between(this.fodselsdato, now()).toInt()
}
