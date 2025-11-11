package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.dagpenger.soknad.orkestrator.behov.Behovløser
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.AndreØkonomiskeYtelser
import no.nav.dagpenger.soknad.orkestrator.behov.Behovmelding
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonRepository

class AndreØkonomiskeYtelserBehovløser(
    rapidsConnection: RapidsConnection,
    opplysningRepository: QuizOpplysningRepository,
    søknadRepository: SøknadRepository,
    seksjonRepository: SeksjonRepository,
) : Behovløser(rapidsConnection, opplysningRepository, søknadRepository, seksjonRepository) {
    override val behov = AndreØkonomiskeYtelser.name
    override val beskrivendeId = "faktum.utbetaling-eller-okonomisk-gode-tidligere-arbeidsgiver"

    override fun løs(behovmelding: Behovmelding) {
        løsBehovFraSeksjonsData(
            behovmelding,
            "annen-pengestotte",
            "får-eller-kommer-til-å-få-lønn-eller-andre-goder-fra-tidligere-arbeidsgiver",
        )
    }
}
