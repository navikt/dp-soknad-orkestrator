package no.nav.dagpenger.soknad.orkestrator.søknad

import no.nav.dagpenger.soknad.orkestrator.api.models.SeksjonDTO
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.QuizOpplysning
import java.util.UUID

class Søknad(
    val søknadId: UUID = UUID.randomUUID(),
    val ident: String,
    val tilstand: Tilstand = Tilstand.PÅBEGYNT,
    val opplysninger: List<QuizOpplysning<*>> = emptyList(),
)

enum class Tilstand {
    PÅBEGYNT,
    INNSENDT,
}

data class OrkestratorSoknadDTO(
    val søknadId: UUID,
    val seksjoner: List<SeksjonDTO>,
    val antallSeksjoner: Int,
    val erFullført: Boolean,
)
