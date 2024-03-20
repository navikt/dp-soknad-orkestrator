package no.nav.dagpenger.soknad.orkestrator.søknad

import com.fasterxml.jackson.databind.JsonNode
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysning
import no.nav.dagpenger.soknad.orkestrator.utils.asUUID

class SøknadMapper(private val jsonNode: JsonNode) {
    val søknad by lazy {
        val ident = jsonNode.get("ident").asText()
        val søknadId = jsonNode.get("søknadId").asUUID()
        val søknadstidspunkt = jsonNode.get("søknadstidspunkt").asText()
        val søknadData = jsonNode.get("søknadData")

        val søknadstidspunktOpplysning =
            Opplysning(
                beskrivendeId = "søknadstidspunkt",
                svar = søknadstidspunkt,
                ident = ident,
                søknadsId = søknadId,
            )

        val opplysninger = mutableListOf<Opplysning>()
        opplysninger.addAll(faktaTilOpplysninger(søknadData, ident))
        opplysninger.add(søknadstidspunktOpplysning)

        Søknad(
            id = søknadId,
            ident = ident,
            opplysninger = opplysninger,
        )
    }

    private fun faktaTilOpplysninger(
        søknadData: JsonNode,
        ident: String,
    ): List<Opplysning> {
        val seksjoner =
            søknadData["seksjoner"]
                ?: throw IllegalArgumentException("Mangler seksjoner")

        return seksjoner.asIterable().map { seksjon ->
            val fakta = seksjon.get("fakta") ?: throw IllegalArgumentException("Mangler fakta")
            fakta.asIterable().map { faktum ->
                val beskrivendeId = faktum.get("beskrivendeId").asText()
                val type = faktum.get("type").asText()

                val svar =
                    when (type) {
                        // TODO - Håndter disse faktumtypene
                        "generator" -> ""
                        "periode" -> ""
                        else -> faktum.get("svar").asText()
                    }
                Opplysning(beskrivendeId, svar, ident)
            }
        }.flatten()
    }
}
