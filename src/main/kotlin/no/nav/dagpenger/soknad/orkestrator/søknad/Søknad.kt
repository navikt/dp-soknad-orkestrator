package no.nav.dagpenger.soknad.orkestrator.søknad

import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.QuizOpplysning
import java.time.LocalDateTime
import java.util.UUID

class Søknad(
    val søknadId: UUID = UUID.randomUUID(),
    val ident: String,
    val tilstand: Tilstand = Tilstand.PÅBEGYNT,
    val opplysninger: List<QuizOpplysning<*>> = emptyList(),
    val innsendtTidspunkt: LocalDateTime? = null,
    val journalpostId: String? = null,
    val journalførtTidspunkt: LocalDateTime? = null,
)

enum class Tilstand {
    PÅBEGYNT,
    INNSENDT,
    JOURNALFØRT,
}
