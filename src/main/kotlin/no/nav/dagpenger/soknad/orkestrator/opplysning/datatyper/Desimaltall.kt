package no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper

import com.fasterxml.jackson.databind.JsonNode
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysning
import java.util.UUID

data object Desimaltall : Datatype<Double>(Double::class.java) {
    override fun tilOpplysning(
        faktum: JsonNode,
        beskrivendeId: String,
        ident: String,
        søknadId: UUID,
    ): Opplysning<*> {
        val svar = faktum.get("svar").asDouble()
        return Opplysning(beskrivendeId, Desimaltall, svar, ident, søknadId)
    }
}
