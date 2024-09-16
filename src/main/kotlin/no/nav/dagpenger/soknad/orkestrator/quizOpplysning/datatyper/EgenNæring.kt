package no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper

import com.fasterxml.jackson.databind.JsonNode
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.QuizOpplysning
import java.util.UUID

@Suppress("UNCHECKED_CAST")
data object EgenNæring : Datatype<List<Int>>(Int::class.java as Class<List<Int>>) {
    override fun tilOpplysning(
        faktum: JsonNode,
        beskrivendeId: String,
        ident: String,
        søknadId: UUID,
    ): QuizOpplysning<*> {
        val svar = faktum.get("svar").flatten().map { it["svar"].asInt() }
        return QuizOpplysning(beskrivendeId, EgenNæring, svar, ident, søknadId)
    }
}
