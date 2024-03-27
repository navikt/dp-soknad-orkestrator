package no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper

import com.fasterxml.jackson.databind.JsonNode
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysning
import java.util.UUID

@Suppress("UNCHECKED_CAST")
data object Flervalg : Datatype<List<String>>(String::class.java as Class<List<String>>) {
    override fun tilOpplysning(
        faktum: JsonNode,
        beskrivendeId: String,
        ident: String,
        søknadId: UUID,
    ): Opplysning<*> {
        val svar = faktum.get("svar").map { it.asText() }
        return Opplysning(beskrivendeId, Flervalg, svar, ident, søknadId)
    }
}
