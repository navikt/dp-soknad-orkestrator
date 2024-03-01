package no.nav.dagpenger.soknad.orkestrator.søknad

import java.time.LocalDateTime
import java.util.UUID

class Opplysning(private val svar: List<String>, private val beskrivendeId: String) {
    fun svar() = svar

    fun beskrivendeId() = beskrivendeId
}

class Søknad(
    val id: UUID,
    val fødselsnummer: String,
    val søknadstidspunkt: LocalDateTime,
    val journalpostId: String,
    val opplysninger: List<Opplysning>,
)
