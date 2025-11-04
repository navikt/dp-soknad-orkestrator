package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.TextNode
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.dagpenger.soknad.orkestrator.behov.Behovløser
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.Verneplikt
import no.nav.dagpenger.soknad.orkestrator.behov.Behovmelding
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonRepository

class VernepliktBehovløser(
    rapidsConnection: RapidsConnection,
    opplysningRepository: QuizOpplysningRepository,
    val seksjonRepository: SeksjonRepository,
) : Behovløser(rapidsConnection, opplysningRepository) {
    override val behov = Verneplikt.name
    override val beskrivendeId = "faktum.avtjent-militaer-sivilforsvar-tjeneste-siste-12-mnd"

    override fun løs(behovmelding: Behovmelding) {
        val svarPåBehov =
            opplysningRepository.hent(beskrivendeId, behovmelding.ident, behovmelding.søknadId)?.svar

        if (svarPåBehov != null) {
            publiserLøsning(behovmelding, svarPåBehov)
        } else {
            val seksjonsSvar =
                seksjonRepository.hentSeksjonsvar(
                    behovmelding.ident,
                    behovmelding.søknadId,
                    "verneplikt",
                ) ?: throw IllegalStateException(
                    "Fant ingen opplysning med beskrivendeId: $beskrivendeId " +
                        "og kan ikke svare på behov $behov for søknad med id: ${behovmelding.søknadId}",
                )

            var avtjentVernepliktValue: Boolean? = null
            objectMapper.readTree(seksjonsSvar).let { seksjonsJson ->
                (seksjonsJson["seksjon"] as? JsonNode)?.let { seksjonsData ->
                    val avtjentVerneplikt = seksjonsData["avtjent-verneplikt"] as TextNode
                    avtjentVernepliktValue = avtjentVerneplikt.asText() == "ja"
                }
            }

            if (avtjentVernepliktValue == null) {
                throw IllegalStateException(
                    "Fant ingen opplysning med beskrivendeId: $beskrivendeId " +
                        "og kan ikke svare på behov $behov for søknad med id: ${behovmelding.søknadId}",
                )
            }

            publiserLøsning(behovmelding, avtjentVernepliktValue)
        }
    }
}
