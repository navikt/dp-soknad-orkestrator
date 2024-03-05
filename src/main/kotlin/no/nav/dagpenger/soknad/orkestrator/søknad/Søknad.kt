package no.nav.dagpenger.soknad.orkestrator.søknad

import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysning
import java.time.LocalDateTime
import java.util.UUID

class Søknad(
    val id: UUID,
    val fødselsnummer: String,
    val søknadstidspunkt: LocalDateTime,
    val journalpostId: String,
    val opplysninger: List<Opplysning>,
)
