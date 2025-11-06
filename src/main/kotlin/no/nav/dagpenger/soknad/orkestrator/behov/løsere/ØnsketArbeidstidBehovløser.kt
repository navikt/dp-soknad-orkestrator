package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.dagpenger.soknad.orkestrator.behov.Behovløser
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.ØnsketArbeidstid
import no.nav.dagpenger.soknad.orkestrator.behov.Behovmelding
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository

class ØnsketArbeidstidBehovløser(
    rapidsConnection: RapidsConnection,
    opplysningRepository: QuizOpplysningRepository,
    søknadRepository: SøknadRepository,
) : Behovløser(rapidsConnection, opplysningRepository, søknadRepository) {
    override val behov = ØnsketArbeidstid.name
    override val beskrivendeId = "faktum.kun-deltid-aarsak-antall-timer"

    override fun løs(behovmelding: Behovmelding) {
        val opplysning =
            opplysningRepository.hent(beskrivendeId, behovmelding.ident, behovmelding.søknadId)

        val svarPåBehov = opplysning?.svar ?: 40.0
        publiserLøsning(behovmelding, svarPåBehov)
    }
}
