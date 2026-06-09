package no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper

import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.QuizOpplysning
import tools.jackson.databind.JsonNode
import java.util.UUID

@Suppress("UNCHECKED_CAST")
data object Flervalg : Datatype<List<String>>(String::class.java as Class<List<String>>) {
    override fun tilOpplysning(
        faktum: JsonNode,
        beskrivendeId: String,
        ident: String,
        søknadId: UUID,
    ): QuizOpplysning<*> {
        val svar = faktum.get("svar").values().map { it.asString() }
        return QuizOpplysning(beskrivendeId, Flervalg, svar, ident, søknadId)
    }
}
