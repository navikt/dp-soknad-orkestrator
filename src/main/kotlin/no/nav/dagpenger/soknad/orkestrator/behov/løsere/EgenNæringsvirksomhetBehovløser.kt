package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.dagpenger.soknad.orkestrator.behov.Behovløser
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.EgenNæringsvirksomhet
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepository

class EgenNæringsvirksomhetBehovløser(
    rapidsConnection: RapidsConnection,
    opplysningRepository: QuizOpplysningRepository,
) : Behovløser(rapidsConnection, opplysningRepository) {
    override val behov = EgenNæringsvirksomhet.name
    override val beskrivendeId = "faktum.driver-du-egen-naering"
}
