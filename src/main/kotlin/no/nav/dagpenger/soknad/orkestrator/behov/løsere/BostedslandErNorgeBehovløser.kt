package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.dagpenger.soknad.orkestrator.behov.Behovløser
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.BostedslandErNorge
import no.nav.dagpenger.soknad.orkestrator.behov.Behovmelding
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonRepository
import no.nav.dagpenger.soknad.orkestrator.utils.erBoolean
import java.util.UUID

class BostedslandErNorgeBehovløser(
    rapidsConnection: RapidsConnection,
    opplysningRepository: QuizOpplysningRepository,
    søknadRepository: SøknadRepository,
    seksjonRepository: SeksjonRepository,
) : Behovløser(rapidsConnection, opplysningRepository, søknadRepository, seksjonRepository) {
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
            opplysningRepository.hent(beskrivendeId, ident, søknadId)

        if (bostedslandOpplysning != null) {
            return bostedslandOpplysning.svar as String == landkodeNorge
        }

        val seksjonsSvar =
            seksjonRepository.hentSeksjonsvarEllerKastException(
                ident,
                søknadId,
                "personalia",
            )

        objectMapper.readTree(seksjonsSvar).let { seksjonsJson ->
            seksjonsJson.findPath("folkeregistrertAdresseErNorgeStemmerDet")?.let {
                if (!it.isMissingOrNull()) {
                    return it.erBoolean()
                }
            }
        }

        throw IllegalStateException(
            "Fant ikke bostedsland for $søknadId",
        )
    }
}
