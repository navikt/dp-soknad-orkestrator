package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.dagpenger.soknad.orkestrator.behov.Behovløser
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.KanJobbeDeltid
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepository

class KanJobbeDeltidBehovløser(
    rapidsConnection: RapidsConnection,
    opplysningRepository: QuizOpplysningRepository,
) : Behovløser(rapidsConnection, opplysningRepository) {
    override val behov = KanJobbeDeltid.name
    override val beskrivendeId = "faktum.jobbe-hel-deltid"
}
