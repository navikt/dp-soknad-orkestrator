package no.nav.dagpenger.soknad.orkestrator.behov.permittering

import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.asListOf
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.ArbeidsforholdSvar
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Sluttårsak
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonRepository
import java.util.UUID

class PermittertVurdering(
    private val opplysningRepository: QuizOpplysningRepository,
    private val seksjonRepository: SeksjonRepository,
) {
    fun rettTilDagpenger(
        ident: String,
        søknadId: UUID,
    ): Boolean {
        val arbeidsforholdOpplysning = opplysningRepository.hent("faktum.arbeidsforhold", ident, søknadId)

        if (arbeidsforholdOpplysning != null) {
            val arbeidsforhold = arbeidsforholdOpplysning.svar.asListOf<ArbeidsforholdSvar>()
            return arbeidsforhold.any { it.sluttårsak == Sluttårsak.PERMITTERT } &&
                arbeidsforhold.none { it.sluttårsak == Sluttårsak.PERMITTERT_FISKEFOREDLING }
        }

        val seksjonsSvar =
            try {
                seksjonRepository.hentSeksjonsvarEllerKastException(
                    ident,
                    søknadId,
                    "arbeidsforhold",
                )
            } catch (e: IllegalStateException) {
                return false
            }

        objectMapper.readTree(seksjonsSvar).let { seksjonsJson ->
            seksjonsJson.findPath("registrerteArbeidsforhold")?.let {
                if (!it.isMissingOrNull()) {
                    return it.any { arbeidsforhold -> arbeidsforhold.erPermittert() } &&
                        it.none { arbeidsforhold -> arbeidsforhold.erFiskepermittert() }
                }
            }
        }
        return false
    }
}
