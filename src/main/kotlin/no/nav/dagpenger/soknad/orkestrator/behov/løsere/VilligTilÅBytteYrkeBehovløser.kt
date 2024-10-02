package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.dagpenger.soknad.orkestrator.behov.Behovløser
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.VilligTilÅBytteYrke
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepository

class VilligTilÅBytteYrkeBehovløser(
    rapidsConnection: RapidsConnection,
    opplysningRepository: QuizOpplysningRepository,
) : Behovløser(rapidsConnection, opplysningRepository) {
    override val behov = VilligTilÅBytteYrke.name
    override val beskrivendeId = "faktum.bytte-yrke-ned-i-lonn"
}
