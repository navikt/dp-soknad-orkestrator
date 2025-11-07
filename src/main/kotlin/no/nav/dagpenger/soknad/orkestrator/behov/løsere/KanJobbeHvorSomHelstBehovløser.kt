package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.dagpenger.soknad.orkestrator.behov.Behovløser
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.KanJobbeHvorSomHelst
import no.nav.dagpenger.soknad.orkestrator.behov.Behovmelding
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonRepository
import no.nav.dagpenger.soknad.orkestrator.utils.erBoolean

class KanJobbeHvorSomHelstBehovløser(
    rapidsConnection: RapidsConnection,
    opplysningRepository: QuizOpplysningRepository,
    søknadRepository: SøknadRepository,
    seksjonRepository: SeksjonRepository,
) : Behovløser(rapidsConnection, opplysningRepository, søknadRepository, seksjonRepository) {
    override val behov = KanJobbeHvorSomHelst.name
    override val beskrivendeId = "faktum.jobbe-hele-norge"

    override fun løs(behovmelding: Behovmelding) {
        val svarPåBehov =
            opplysningRepository.hent(beskrivendeId, behovmelding.ident, behovmelding.søknadId)?.svar

        if (svarPåBehov != null) {
            return publiserLøsning(behovmelding, svarPåBehov)
        }
        val seksjonsSvar =
            seksjonRepository.hentSeksjonsvar(
                behovmelding.ident,
                behovmelding.søknadId,
                "reell-arbeidssoker",
            ) ?: throw IllegalStateException(
                "Fant ingen seksjonsvar på Reell Arbeidssøker for søknad=${behovmelding.søknadId}",
            )

        objectMapper.readTree(seksjonsSvar).let { seksjonsJson ->
            seksjonsJson.findPath("kan-du-jobbe-i-hele-norge")?.let {
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
