package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import com.fasterxml.jackson.databind.node.TextNode
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.dagpenger.soknad.orkestrator.behov.Behovløser
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.HarTilleggsopplysninger
import no.nav.dagpenger.soknad.orkestrator.behov.Behovmelding
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonRepository

class HarTilleggsopplysningerBehovløser(
    rapidsConnection: RapidsConnection,
    opplysningRepository: QuizOpplysningRepository,
    søknadRepository: SøknadRepository,
    seksjonRepository: SeksjonRepository,
) : Behovløser(rapidsConnection, opplysningRepository, søknadRepository, seksjonRepository) {
    override val behov = HarTilleggsopplysninger.name
    override val beskrivendeId = "faktum.tilleggsopplysninger.har-tilleggsopplysninger"

    override fun løs(behovmelding: Behovmelding) {
        val quizOpplysningsvar =
            opplysningRepository.hent(beskrivendeId, behovmelding.ident, behovmelding.søknadId)?.svar

        if (quizOpplysningsvar != null) {
            publiserLøsning(behovmelding, quizOpplysningsvar)
            return
        }

        val seksjonsvar =
            seksjonRepository?.hentSeksjonsvar(
                behovmelding.ident,
                behovmelding.søknadId,
                "tilleggsopplysninger",
            ) ?: throw IllegalStateException(
                "Fant ingen opplysning med beskrivendeId: $beskrivendeId " +
                    "og kan ikke svare på behov $behov for søknad med id: ${behovmelding.søknadId}",
            )

        val harTilleggsopplysninger =
            objectMapper.readTree(seksjonsvar).let { seksjonsJson ->
                seksjonsJson["seksjon"]?.let { seksjonsData ->
                    val harTilleggsopplysninger = seksjonsData["har-tilleggsopplysninger"] as TextNode
                    harTilleggsopplysninger.asText() == "ja"
                }
            }

        if (harTilleggsopplysninger == null) {
            throw IllegalStateException(
                "Fant ingen opplysning med beskrivendeId: $beskrivendeId " +
                    "og kan ikke svare på behov $behov for søknad med id: ${behovmelding.søknadId}",
            )
        }

        publiserLøsning(behovmelding, harTilleggsopplysninger)
    }
}
