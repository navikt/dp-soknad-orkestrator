package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.dagpenger.soknad.orkestrator.behov.Behovløser
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.Sanksjon
import no.nav.dagpenger.soknad.orkestrator.behov.Behovmelding
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.asListOf
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.ArbeidsforholdSvar
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Sluttårsak
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonRepository
import java.util.UUID

class SanksjonBehovløser(
    rapidsConnection: RapidsConnection,
    opplysningRepository: QuizOpplysningRepository,
    søknadRepository: SøknadRepository,
    seksjonRepository: SeksjonRepository,
) : Behovløser(rapidsConnection, opplysningRepository, søknadRepository, seksjonRepository) {
    override val behov = Sanksjon.name
    override val beskrivendeId = "faktum.arbeidsforhold"

    override fun løs(behovmelding: Behovmelding) {
        val svarPåBehov = harSanksjon(behovmelding.ident, behovmelding.søknadId)
        publiserLøsning(behovmelding, svarPåBehov)
    }

    internal fun harSanksjon(
        ident: String,
        søknadId: UUID,
    ): Boolean {
        val arbeidsforholdOpplysning = opplysningRepository.hent(beskrivendeId, ident, søknadId)

        val sanksjonSluttårsakerIQuiz =
            setOf(
                Sluttårsak.SAGT_OPP_AV_ARBEIDSGIVER,
                Sluttårsak.SAGT_OPP_SELV,
                Sluttårsak.AVSKJEDIGET,
            )

        if (arbeidsforholdOpplysning != null) {
            return arbeidsforholdOpplysning.svar.asListOf<ArbeidsforholdSvar>().any { arbeidsforhold ->
                arbeidsforhold.sluttårsak in sanksjonSluttårsakerIQuiz
            }
        }

        val seksjonsSvar = seksjonRepository.hentSeksjonsvarEllerKastException(ident, søknadId, "arbeidsforhold")

        val sanksjonSluttårsakerIOrkestrator =
            setOf(
                "arbeidsgiverenMinHarSagtMegOpp",
                "jegHarSagtOppSelv",
                "jegHarFåttAvskjed",
            )

        objectMapper.readTree(seksjonsSvar).let { seksjonsJson ->
            seksjonsJson.findPath("registrerteArbeidsforhold")?.let {
                if (!it.isMissingOrNull()) {
                    return it.any { arbeidsforhold ->
                        arbeidsforhold["hvordanHarDetteArbeidsforholdetEndretSeg"]?.asText() in sanksjonSluttårsakerIOrkestrator
                    }
                }
            }
        }
        return false
    }
}
