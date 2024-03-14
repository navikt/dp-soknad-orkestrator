package no.nav.dagpenger.soknad.orkestrator.søknad

import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysning
import no.nav.helse.rapids_rivers.JsonMessage
import java.util.UUID

class SøknadMapper(private val packet: JsonMessage) {
    val søknad by lazy {
        val id =
            UUID.fromString(packet["@id"].asText())
                ?: throw IllegalArgumentException("Mangler id")
        val ident =
            packet["fødselsnummer"].asText()
                ?: throw IllegalArgumentException("Mangler fødselsnummer")

        val seksjoner =
            packet["søknadsData"].get("seksjoner")
                ?: throw IllegalArgumentException("Mangler seksjoner")

        val opplysninger =
            seksjoner.asIterable().map { seksjon ->
                val fakta = seksjon.get("fakta") ?: throw IllegalArgumentException("Mangler fakta")
                fakta.asIterable().map { faktum ->
                    val beskrivendeId = faktum.get("beskrivendeId").asText()
                    val type = faktum.get("type").asText()

                    val svar =
                        when (type) {
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
