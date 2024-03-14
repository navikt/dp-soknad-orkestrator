package no.nav.dagpenger.soknad.orkestrator.søknad

import com.fasterxml.jackson.databind.JsonNode
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysning
import java.util.UUID

class SøknadMapper(private val jsonNode: JsonNode) {
    val søknad by lazy {
        val ident = jsonNode.get("fødselsnummer").asText()
        val søknadsData = jsonNode.get("søknadsData") ?: throw IllegalArgumentException("Mangler søknadsData")
        val id =
            søknadsData["søknad_uuid"]?.let {
                UUID.fromString(it.asText())
            } ?: throw IllegalArgumentException("Mangler søknad_uuid")

        val seksjoner =
            søknadsData["seksjoner"]
                ?: throw IllegalArgumentException("Mangler seksjoner")

        val opplysninger =
            seksjoner.asIterable().map { seksjon ->
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
            }

        Søknad(
            id = id,
            ident = ident,
            opplysninger = opplysninger.flatten(),
        )
    }
}
