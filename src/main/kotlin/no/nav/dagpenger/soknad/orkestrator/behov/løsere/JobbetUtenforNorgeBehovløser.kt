package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import no.nav.dagpenger.soknad.orkestrator.behov.Behovløser
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.JobbetUtenforNorge
import no.nav.dagpenger.soknad.orkestrator.behov.Behovmelding
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.asListOf
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.ArbeidsforholdSvar
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepository
import no.nav.helse.rapids_rivers.RapidsConnection
import java.util.UUID

class JobbetUtenforNorgeBehovløser(
    rapidsConnection: RapidsConnection,
    opplysningRepository: QuizOpplysningRepository,
) :
    Behovløser(rapidsConnection, opplysningRepository) {
    override val behov = JobbetUtenforNorge.name
    override val beskrivendeId = "faktum.arbeidsforhold"
    private val landkodeNorge = "NOR"

    override fun løs(behovmelding: Behovmelding) {
        val svarPåBehov = harJobbetUtenforNorge(behovmelding.ident, behovmelding.søknadId)
        publiserLøsning(behovmelding, svarPåBehov)
    }

    internal fun harJobbetUtenforNorge(
        ident: String,
        søknadId: UUID,
    ): Boolean {
        val arbeidsforholdOpplysning = opplysningRepository.hent(beskrivendeId, ident, søknadId) ?: return false
        return arbeidsforholdOpplysning.svar.asListOf<ArbeidsforholdSvar>().any { it.land != landkodeNorge }
    }
}
