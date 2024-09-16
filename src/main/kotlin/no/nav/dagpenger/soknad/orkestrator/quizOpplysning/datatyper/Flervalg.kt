package no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper

import com.fasterxml.jackson.databind.JsonNode
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.QuizOpplysning
import java.util.UUID

@Suppress("UNCHECKED_CAST")
data object Flervalg : Datatype<List<String>>(String::class.java as Class<List<String>>) {
    override fun tilOpplysning(
        faktum: JsonNode,
        beskrivendeId: String,
        ident: String,
        søknadId: UUID,
    ): QuizOpplysning<*> {
        val svar = faktum.get("svar").map { it.asText() }
        return QuizOpplysning(beskrivendeId, Flervalg, svar, ident, søknadId)
    }
}
