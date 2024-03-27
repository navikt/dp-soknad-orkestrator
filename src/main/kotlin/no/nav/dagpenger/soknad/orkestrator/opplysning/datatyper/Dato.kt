package no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper

import com.fasterxml.jackson.databind.JsonNode
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysning
import no.nav.helse.rapids_rivers.asLocalDate
import java.time.LocalDate
import java.util.UUID

data object Dato : Datatype<LocalDate>(LocalDate::class.java) {
    override fun tilOpplysning(
        faktum: JsonNode,
        beskrivendeId: String,
        ident: String,
        søknadId: UUID,
    ): Opplysning<*> {
        val svar = faktum.get("svar").asLocalDate()
        return Opplysning(beskrivendeId, Dato, svar, ident, søknadId)
    }
}
