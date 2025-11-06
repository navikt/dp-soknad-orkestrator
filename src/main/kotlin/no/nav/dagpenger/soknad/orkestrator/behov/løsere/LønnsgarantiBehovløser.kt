package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.dagpenger.soknad.orkestrator.behov.Behovløser
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.Lønnsgaranti
import no.nav.dagpenger.soknad.orkestrator.behov.Behovmelding
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.asListOf
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.ArbeidsforholdSvar
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Sluttårsak
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonRepository
import java.util.UUID

class LønnsgarantiBehovløser(
    rapidsConnection: RapidsConnection,
    opplysningRepository: QuizOpplysningRepository,
    søknadRepository: SøknadRepository,
    seksjonRepository: SeksjonRepository,
) : Behovløser(rapidsConnection, opplysningRepository, søknadRepository, seksjonRepository) {
    override val behov = Lønnsgaranti.name
    override val beskrivendeId = "faktum.arbeidsforhold"

    override fun løs(behovmelding: Behovmelding) {
        val svarPåBehov = rettTilDagpengerEtterKonkurs(behovmelding.ident, behovmelding.søknadId)
        publiserLøsning(behovmelding, svarPåBehov)
    }

    internal fun rettTilDagpengerEtterKonkurs(
        ident: String,
        søknadId: UUID,
    ): Boolean {
        val arbeidsforholdOpplysning = opplysningRepository.hent(beskrivendeId, ident, søknadId)
        if (arbeidsforholdOpplysning != null) {
            return arbeidsforholdOpplysning.svar.asListOf<ArbeidsforholdSvar>().any {
                it.sluttårsak == Sluttårsak.ARBEIDSGIVER_KONKURS
            }
        }

        val seksjonsSvar =
            seksjonRepository?.hentSeksjonsvar(
                ident,
                søknadId,
                "arbeidsforhold",
            ) ?: return false

        objectMapper.readTree(seksjonsSvar).let { seksjonsJson ->
            seksjonsJson.findPath("registrerteArbeidsforhold")?.let {
                if (!it.isMissingOrNull()) {
                    return it.any { arbeidsforhold ->
                        arbeidsforhold["hvordan-har-dette-arbeidsforholdet-endret-seg"].asText() == "arbeidsgiver-er-konkurs"
                    }
                }
            }
        }
        return false
    }
}
