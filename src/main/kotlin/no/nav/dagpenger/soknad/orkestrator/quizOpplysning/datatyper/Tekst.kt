package no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper

import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.QuizOpplysning
import tools.jackson.databind.JsonNode
import java.util.UUID

data object Tekst : Datatype<String>(String::class.java) {
    override fun tilOpplysning(
        faktum: JsonNode,
        beskrivendeId: String,
        ident: String,
        søknadId: UUID,
    ): QuizOpplysning<*> {
        val svar = faktum.get("svar").asString()
        return QuizOpplysning(beskrivendeId, Tekst, svar, ident, søknadId)
    }
}
