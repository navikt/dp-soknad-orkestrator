package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.dagpenger.soknad.orkestrator.behov.Behovløser
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.PlanleggerUtdanning
import no.nav.dagpenger.soknad.orkestrator.behov.Behovmelding
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonRepository
import no.nav.dagpenger.soknad.orkestrator.utils.erBoolean
import java.util.UUID

class PlanleggerUtdanningBehovløser(
    rapidsConnection: RapidsConnection,
    opplysningRepository: QuizOpplysningRepository,
    søknadRepository: SøknadRepository,
    seksjonRepository: SeksjonRepository,
) : Behovløser(rapidsConnection, opplysningRepository, søknadRepository, seksjonRepository) {
    override val behov = PlanleggerUtdanning.name
    override val beskrivendeId = "faktum.planlegger-utdanning-med-dagpenger"

    override fun løs(behovmelding: Behovmelding) {
        publiserLøsning(behovmelding, planleggerUtdanning(behovmelding.ident, behovmelding.søknadId))
    }

    internal fun planleggerUtdanning(
        ident: String,
        søknadId: UUID,
    ): Boolean {
        val quizOpplysning = opplysningRepository.hent(beskrivendeId, ident, søknadId)
        if (quizOpplysning != null) {
            return quizOpplysning.svar as Boolean
        }

        val seksjonsSvar = seksjonRepository.hentSeksjonsvarEllerKastException(ident, søknadId, "utdanning")
        val json = objectMapper.readTree(seksjonsSvar)

        if (json.findPath("tarUtdanningEllerOpplæring").erBoolean()) return false

        return json.findPath("planleggerÅStarteEllerFullføreStudierSamtidig").erBoolean()
    }
}
