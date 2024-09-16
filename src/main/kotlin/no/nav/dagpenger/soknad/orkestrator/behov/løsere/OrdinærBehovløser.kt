package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import no.nav.dagpenger.soknad.orkestrator.behov.Behovløser
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.Ordinær
import no.nav.dagpenger.soknad.orkestrator.behov.Behovmelding
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.asListOf
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.ArbeidsforholdSvar
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Sluttårsak
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepository
import no.nav.helse.rapids_rivers.RapidsConnection
import java.util.UUID

class OrdinærBehovløser(rapidsConnection: RapidsConnection, opplysningRepository: QuizOpplysningRepository) :
    Behovløser(rapidsConnection, opplysningRepository) {
    override val behov = Ordinær.name
    override val beskrivendeId = "faktum.arbeidsforhold"

    override fun løs(behovmelding: Behovmelding) {
        val svarPåBehov = rettTilOrdinæreDagpenger(behovmelding.ident, behovmelding.søknadId)
        publiserLøsning(behovmelding, svarPåBehov)
    }

    internal fun rettTilOrdinæreDagpenger(
        ident: String,
        søknadId: UUID,
    ): Boolean {
        val arbeidsforholdOpplysning = opplysningRepository.hent(beskrivendeId, ident, søknadId) ?: return false

        val ikkeOrdinæreRettighetstyper =
            setOf(Sluttårsak.PERMITTERT, Sluttårsak.PERMITTERT_FISKEFOREDLING, Sluttårsak.ARBEIDSGIVER_KONKURS)

        return arbeidsforholdOpplysning.svar.asListOf<ArbeidsforholdSvar>().any {
            it.sluttårsak !in ikkeOrdinæreRettighetstyper
        }
    }
}
