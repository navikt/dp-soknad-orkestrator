package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.dagpenger.soknad.orkestrator.behov.Behovløser
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.ØnskerDagpengerFraDato
import no.nav.dagpenger.soknad.orkestrator.behov.Behovmelding
import no.nav.dagpenger.soknad.orkestrator.behov.FellesBehovløserLøsninger
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonRepository

class ØnskerDagpengerFraDatoBehovløser(
    rapidsConnection: RapidsConnection,
    opplysningRepository: QuizOpplysningRepository,
    søknadRepository: SøknadRepository,
    seksjonRepository: SeksjonRepository,
    fellesBehovLøserLøsninger: FellesBehovløserLøsninger,
) : Behovløser(rapidsConnection, opplysningRepository, søknadRepository, seksjonRepository, fellesBehovLøserLøsninger) {
    override val behov = ØnskerDagpengerFraDato.name
    override val beskrivendeId
        get() = throw NotImplementedError("Overrider løs() og trenger ikke beskrivendeId")

    override fun løs(behovmelding: Behovmelding) {
        val dato =
            fellesBehovløserLøsninger!!.ønskerDagpengerFraDato(
                behovmelding.ident,
                behovmelding.søknadId,
                behov,
            )
        return publiserLøsning(behovmelding, dato)
    }
}
