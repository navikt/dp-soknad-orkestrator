package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.dagpenger.soknad.orkestrator.behov.Behovløser
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.PermittertGrensearbeider
import no.nav.dagpenger.soknad.orkestrator.behov.Behovmelding
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepository
import java.util.UUID

class PermittertGrensearbeiderBehovløser(
    rapidsConnection: RapidsConnection,
    opplysningRepository: QuizOpplysningRepository,
) : Behovløser(rapidsConnection, opplysningRepository) {
    override val behov = PermittertGrensearbeider.name
    override val beskrivendeId
        get() = throw NotImplementedError("Overrider løs() og trenger ikke beskrivendeId")

    companion object {
        val reistTilbakeEnGangEllerMerBeskrivendeId = "faktum.reist-tilbake-en-gang-eller-mer"
        val reistITaktMedRotasjonBeskrivendeId = "faktum.reist-i-takt-med-rotasjon"
    }

    override fun løs(behovmelding: Behovmelding) {
        val svarPåBehov = harReistTilbake(behovmelding.ident, behovmelding.søknadId)
        publiserLøsning(behovmelding, svarPåBehov)
    }

    private fun harReistTilbake(
        ident: String,
        søknadId: UUID,
    ): Boolean {
        val reistTilbakeEnGangEllerMerOpplysning =
            opplysningRepository.hent(reistTilbakeEnGangEllerMerBeskrivendeId, ident, søknadId)

        val reistITaktMedRotasjonOpplysning =
            opplysningRepository.hent(reistITaktMedRotasjonBeskrivendeId, ident, søknadId)

        return reistTilbakeEnGangEllerMerOpplysning?.svar as? Boolean ?: false ||
            reistITaktMedRotasjonOpplysning?.svar as? Boolean ?: false
    }
}
