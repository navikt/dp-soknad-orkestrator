package no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper

import com.fasterxml.jackson.databind.JsonNode
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.QuizOpplysning
import java.util.UUID

data object Desimaltall : Datatype<Double>(Double::class.java) {
    override fun tilOpplysning(
        faktum: JsonNode,
        beskrivendeId: String,
        ident: String,
        søknadId: UUID,
    ): QuizOpplysning<*> {
        val svar = faktum.get("svar").asDouble()
        return QuizOpplysning(beskrivendeId, Desimaltall, svar, ident, søknadId)
    }
}
