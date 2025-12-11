package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.dagpenger.soknad.orkestrator.behov.Behovløser
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.Verneplikt
import no.nav.dagpenger.soknad.orkestrator.behov.Behovmelding
import no.nav.dagpenger.soknad.orkestrator.behov.FellesBehovløserLøsninger
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonRepository

class VernepliktBehovløser(
    rapidsConnection: RapidsConnection,
    opplysningRepository: QuizOpplysningRepository,
    søknadRepository: SøknadRepository,
    seksjonRepository: SeksjonRepository,
    fellesBehovLøserLøsninger: FellesBehovløserLøsninger,
) : Behovløser(rapidsConnection, opplysningRepository, søknadRepository, seksjonRepository, fellesBehovLøserLøsninger) {
    override val behov = Verneplikt.name
    override val beskrivendeId = "faktum.avtjent-militaer-sivilforsvar-tjeneste-siste-12-mnd"

    override fun løs(behovmelding: Behovmelding) {
        val harSøkerenAvtjentVerneplikt =
            fellesBehovløserLøsninger!!.harSøkerenAvtjentVerneplikt(
                behov,
                beskrivendeId,
                behovmelding.ident,
                behovmelding.søknadId,
            )

        publiserLøsning(behovmelding, harSøkerenAvtjentVerneplikt)
    }
}
