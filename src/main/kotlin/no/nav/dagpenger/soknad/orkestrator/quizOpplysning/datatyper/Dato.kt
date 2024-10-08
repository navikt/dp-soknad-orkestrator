package no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.QuizOpplysning
import java.time.LocalDate
import java.util.UUID

data object Dato : Datatype<LocalDate>(LocalDate::class.java) {
    override fun tilOpplysning(
        faktum: JsonNode,
        beskrivendeId: String,
        ident: String,
        søknadId: UUID,
    ): QuizOpplysning<*> {
        val svar = faktum.get("svar").asLocalDate()
        return QuizOpplysning(beskrivendeId, Dato, svar, ident, søknadId)
    }
}
