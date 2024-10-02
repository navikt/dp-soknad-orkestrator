package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.dagpenger.soknad.orkestrator.behov.Behovløser
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.Verneplikt
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepository

class VernepliktBehovløser(
    rapidsConnection: RapidsConnection,
    opplysningRepository: QuizOpplysningRepository,
) : Behovløser(rapidsConnection, opplysningRepository) {
    override val behov = Verneplikt.name
    override val beskrivendeId = "faktum.avtjent-militaer-sivilforsvar-tjeneste-siste-12-mnd"
}
