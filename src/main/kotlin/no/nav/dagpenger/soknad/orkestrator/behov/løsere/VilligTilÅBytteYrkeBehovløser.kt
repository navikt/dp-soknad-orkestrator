package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.dagpenger.soknad.orkestrator.behov.Behovløser
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.VilligTilÅBytteYrke
import no.nav.dagpenger.soknad.orkestrator.behov.Behovmelding
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonRepository

class VilligTilÅBytteYrkeBehovløser(
    rapidsConnection: RapidsConnection,
    opplysningRepository: QuizOpplysningRepository,
    søknadRepository: SøknadRepository,
    seksjonRepository: SeksjonRepository,
) : Behovløser(rapidsConnection, opplysningRepository, søknadRepository, seksjonRepository) {
    override val behov = VilligTilÅBytteYrke.name
    override val beskrivendeId = "faktum.bytte-yrke-ned-i-lonn"

    override fun løs(behovmelding: Behovmelding) {
        løsBehovFraSeksjonsData(behovmelding, "reell-arbeidssoker", "erDuVilligTilÅBytteYrkeEllerGåNedILønn")
    }
}
