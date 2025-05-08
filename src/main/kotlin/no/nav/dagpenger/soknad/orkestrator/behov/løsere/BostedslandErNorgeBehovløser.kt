package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.dagpenger.soknad.orkestrator.behov.Behovløser
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.BostedslandErNorge
import no.nav.dagpenger.soknad.orkestrator.behov.Behovmelding
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepository
import java.util.UUID

class BostedslandErNorgeBehovløser(
    rapidsConnection: RapidsConnection,
    opplysningRepository: QuizOpplysningRepository,
) : Behovløser(rapidsConnection, opplysningRepository) {
    override val behov = BostedslandErNorge.name
    override val beskrivendeId = "faktum.hvilket-land-bor-du-i"
    private val landkodeNorge = "NOR"

    override fun løs(behovmelding: Behovmelding) {
        val svarPåBehov = bostedslandErNorge(behovmelding.ident, behovmelding.søknadId)
        publiserLøsning(behovmelding, svarPåBehov)
    }

    internal fun bostedslandErNorge(
        ident: String,
        søknadId: UUID,
    ): Boolean {
        val bostedslandOpplysning =
            opplysningRepository.hent(beskrivendeId, ident, søknadId) ?: throw IllegalStateException(
                "Fant ikke bostedsland for $søknadId",
            )
        return bostedslandOpplysning.svar as String == landkodeNorge
    }
}
