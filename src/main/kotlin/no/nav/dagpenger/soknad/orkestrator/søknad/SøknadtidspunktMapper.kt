package no.nav.dagpenger.soknad.orkestrator.søknad

import com.fasterxml.jackson.databind.JsonNode
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysning
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.Tekst
import no.nav.dagpenger.soknad.orkestrator.utils.asUUID

class SøknadtidspunktMapper(val jsonNode: JsonNode) {
    val tidspunktOpplysning by lazy {
        val ident = jsonNode.get("ident").asText()
        val søknadId = jsonNode.get("søknadId").asUUID()
        val søknadstidspunkt = jsonNode.get("søknadstidspunkt").asText()

        Opplysning(
            beskrivendeId = "søknadstidspunkt",
            type = Tekst,
            svar = søknadstidspunkt,
            ident = ident,
            søknadId = søknadId,
        )
    }
}
