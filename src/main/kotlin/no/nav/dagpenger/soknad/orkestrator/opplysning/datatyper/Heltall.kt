package no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper

import com.fasterxml.jackson.databind.JsonNode
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysning
import java.util.UUID

data object Heltall : Datatype<Int>(Int::class.java) {
    override fun tilOpplysning(
        faktum: JsonNode,
        beskrivendeId: String,
        ident: String,
        søknadId: UUID,
    ): Opplysning<*> {
        val svar = faktum.get("svar").asInt()
        return Opplysning(beskrivendeId, Heltall, svar, ident, søknadId)
    }
}
