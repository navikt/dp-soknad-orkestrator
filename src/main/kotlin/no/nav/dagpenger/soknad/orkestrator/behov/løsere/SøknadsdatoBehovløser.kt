package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.dagpenger.soknad.orkestrator.behov.Behovløser
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.Søknadsdato
import no.nav.dagpenger.soknad.orkestrator.behov.Behovmelding
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonRepository
import java.time.ZonedDateTime

class SøknadsdatoBehovløser(
    rapidsConnection: RapidsConnection,
    opplysningRepository: QuizOpplysningRepository,
    søknadRepository: SøknadRepository,
    seksjonRepository: SeksjonRepository,
) : Behovløser(rapidsConnection, opplysningRepository, søknadRepository, seksjonRepository) {
    override val behov = Søknadsdato.name
    override val beskrivendeId = "søknadstidspunkt"

    override fun løs(behovmelding: Behovmelding) {
        val opplysningFraQuiz =
            opplysningRepository.hent(beskrivendeId, behovmelding.ident, behovmelding.søknadId)

        if (opplysningFraQuiz != null) {
            val svarPåBehov = ZonedDateTime.parse(opplysningFraQuiz.svar as String).toLocalDate()
            return publiserLøsning(behovmelding, svarPåBehov)
        }

        val innsendtTidspunkt = søknadRepository.hent(behovmelding.søknadId)?.innsendtTidspunkt

        if (innsendtTidspunkt != null) {
            val svarPåBehov = innsendtTidspunkt.toLocalDate()
            return publiserLøsning(behovmelding, svarPåBehov)
        }

        throw IllegalStateException("Kan ikke finne søknadsdato for søknad ${behovmelding.søknadId} og behovet $behov")
    }
}
