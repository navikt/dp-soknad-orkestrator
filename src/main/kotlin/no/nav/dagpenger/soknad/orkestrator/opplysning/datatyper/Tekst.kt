package no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper

import com.fasterxml.jackson.databind.JsonNode
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysning
import java.util.UUID

data object Tekst : Datatype<String>(String::class.java) {
    override fun tilOpplysning(
        faktum: JsonNode,
        beskrivendeId: String,
        ident: String,
        søknadId: UUID,
    ): Opplysning<*> {
        val svar = faktum.get("svar").asText()
        return Opplysning(beskrivendeId, Tekst, svar, ident, søknadId)
    }
}
