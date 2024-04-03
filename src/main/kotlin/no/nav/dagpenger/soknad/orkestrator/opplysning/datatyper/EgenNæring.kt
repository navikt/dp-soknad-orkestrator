package no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper

import com.fasterxml.jackson.databind.JsonNode
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysning
import java.util.UUID

@Suppress("UNCHECKED_CAST")
data object EgenNæring : Datatype<List<Int>>(String::class.java as Class<List<Int>>) {
    override fun tilOpplysning(
        faktum: JsonNode,
        beskrivendeId: String,
        ident: String,
        søknadId: UUID,
    ): Opplysning<*> {
        val svar = faktum.get("svar").flatten().map { it["svar"].asInt() }
        return Opplysning(beskrivendeId, EgenNæring, svar, ident, søknadId)
    }
}
