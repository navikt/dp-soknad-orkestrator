package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.dagpenger.soknad.orkestrator.behov.Behovløser
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.HarTilleggsopplysninger
import no.nav.dagpenger.soknad.orkestrator.behov.Behovmelding
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonRepository
import no.nav.dagpenger.soknad.orkestrator.utils.erBoolean

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
            seksjonRepository.hentSeksjonsvar(
                behovmelding.ident,
                behovmelding.søknadId,
                "tilleggsopplysninger",
            ) ?: throw IllegalStateException(
                "Fant ingen opplysning med beskrivendeId: $beskrivendeId " +
                    "og kan ikke svare på behov $behov for søknad med id: ${behovmelding.søknadId}",
            )

        objectMapper.readTree(seksjonsvar).let { seksjonsJson ->
            seksjonsJson.findPath("har-tilleggsopplysninger")?.let {
                if (!it.isMissingOrNull()) {
                    return publiserLøsning(behovmelding, it.erBoolean())
                }
            }
        }
        throw IllegalStateException(
            "Fant ingen opplysning på behov $behov for søknad med id: ${behovmelding.søknadId}",
        )
    }
}
