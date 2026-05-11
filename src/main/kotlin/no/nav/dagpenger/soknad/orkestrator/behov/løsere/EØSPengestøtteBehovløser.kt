package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.dagpenger.soknad.orkestrator.behov.Behovløser
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.EØSPengestøtte
import no.nav.dagpenger.soknad.orkestrator.behov.Behovmelding
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.asListOf
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonRepository
import no.nav.dagpenger.soknad.orkestrator.utils.erBoolean
import java.util.UUID

class EØSPengestøtteBehovløser(
    rapidsConnection: RapidsConnection,
    opplysningRepository: QuizOpplysningRepository,
    søknadRepository: SøknadRepository,
    seksjonRepository: SeksjonRepository,
) : Behovløser(rapidsConnection, opplysningRepository, søknadRepository, seksjonRepository) {
    override val behov = EØSPengestøtte.name
    override val beskrivendeId = "faktum.hvilke-andre-ytelser"

    companion object {
        const val EØS_DAGPENGER_SVAR = "faktum.hvilke-andre-ytelser.svar.dagpenger-annet-eos-land"
    }

    override fun løs(behovmelding: Behovmelding) {
        val svarPåBehov = harEøsPengestøtte(behovmelding.ident, behovmelding.søknadId)
        publiserLøsning(behovmelding, svarPåBehov)
    }

    internal fun harEøsPengestøtte(
        ident: String,
        søknadId: UUID,
    ): Boolean {
        // 1. Quiz-opplysninger (gammel søknad)
        val quizOpplysning = opplysningRepository.hent(beskrivendeId, ident, søknadId)

        if (quizOpplysning != null) {
            return quizOpplysning.svar.asListOf<String>().contains(EØS_DAGPENGER_SVAR)
        }

        // 2. Seksjon (ny søknad)
        val seksjonsSvar = seksjonRepository.hentSeksjonsvarEllerKastException(ident, søknadId, "annen-pengestøtte")

        return objectMapper
            .readTree(seksjonsSvar)
            .findPath("harMottattEllerSøktOmPengestøtteFraAndreEøsLand")
            .let { if (it.isMissingOrNull()) false else it.erBoolean() }
    }
}
