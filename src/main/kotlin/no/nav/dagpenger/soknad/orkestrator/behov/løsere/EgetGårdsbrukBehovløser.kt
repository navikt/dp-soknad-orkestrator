package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.dagpenger.soknad.orkestrator.behov.Behovløser
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.EgetGårdsbruk
import no.nav.dagpenger.soknad.orkestrator.behov.Behovmelding
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonRepository
import no.nav.dagpenger.soknad.orkestrator.utils.erBoolean

class EgetGårdsbrukBehovløser(
    rapidsConnection: RapidsConnection,
    opplysningRepository: QuizOpplysningRepository,
    søknadRepository: SøknadRepository,
    seksjonRepository: SeksjonRepository,
) : Behovløser(rapidsConnection, opplysningRepository, søknadRepository, seksjonRepository) {
    override val behov = EgetGårdsbruk.name
    override val beskrivendeId = "faktum.driver-du-eget-gaardsbruk"

    override fun løs(behovmelding: Behovmelding) {
        val svarPåBehov =
            opplysningRepository.hent(beskrivendeId, behovmelding.ident, behovmelding.søknadId)?.svar

        if (svarPåBehov != null) {
            return publiserLøsning(behovmelding, svarPåBehov)
        }
        val seksjonsSvar =
            seksjonRepository?.hentSeksjonsvar(
                behovmelding.ident,
                behovmelding.søknadId,
                "egen-naring",
            ) ?: throw IllegalStateException(
                "Fant ingen seksjonsvar på Egen næring for søknad=${behovmelding.søknadId}",
            )

        objectMapper.readTree(seksjonsSvar).let { seksjonsJson ->
            seksjonsJson.findPath("driver-du-eget-gårdsbruk")?.let {
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
