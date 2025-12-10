package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.dagpenger.soknad.orkestrator.behov.Behovløser
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.EØSArbeid
import no.nav.dagpenger.soknad.orkestrator.behov.Behovmelding
import no.nav.dagpenger.soknad.orkestrator.behov.FellesBehovløserLøsninger
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonRepository

class EØSArbeidBehovløser(
    rapidsConnection: RapidsConnection,
    opplysningRepository: QuizOpplysningRepository,
    søknadRepository: SøknadRepository,
    seksjonRepository: SeksjonRepository,
    fellesBehovløserLøsninger: FellesBehovløserLøsninger,
) : Behovløser(rapidsConnection, opplysningRepository, søknadRepository, seksjonRepository, fellesBehovløserLøsninger) {
    override val behov = EØSArbeid.name
    override val beskrivendeId = "faktum.eos-arbeid-siste-36-mnd"

    override fun løs(behovmelding: Behovmelding) {
        publiserLøsning(
            behovmelding,
            fellesBehovløserLøsninger!!.harSøkerenHattArbeidsforholdIEøs(
                beskrivendeId,
                behovmelding.ident,
                behovmelding.søknadId,
            ),
        )
    }
}
