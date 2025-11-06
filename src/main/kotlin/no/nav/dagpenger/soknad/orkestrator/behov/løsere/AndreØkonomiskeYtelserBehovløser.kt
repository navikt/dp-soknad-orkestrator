package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.dagpenger.soknad.orkestrator.behov.Behovløser
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.AndreØkonomiskeYtelser
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository

class AndreØkonomiskeYtelserBehovløser(
    rapidsConnection: RapidsConnection,
    opplysningRepository: QuizOpplysningRepository,
    søknadRepository: SøknadRepository,
) : Behovløser(rapidsConnection, opplysningRepository, søknadRepository) {
    override val behov = AndreØkonomiskeYtelser.name
    override val beskrivendeId = "faktum.utbetaling-eller-okonomisk-gode-tidligere-arbeidsgiver"
}
