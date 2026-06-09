package no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper

import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.QuizOpplysning
import tools.jackson.databind.JsonNode
import java.util.UUID

@Suppress("UNCHECKED_CAST")
data object EøsArbeidsforhold : Datatype<List<EøsArbeidsforholdSvar>>(
    List::class.java as Class<List<EøsArbeidsforholdSvar>>,
) {
    override fun tilOpplysning(
        faktum: JsonNode,
        beskrivendeId: String,
        ident: String,
        søknadId: UUID,
    ): QuizOpplysning<*> {
        val eøsArbeidsforholdSvar: List<EøsArbeidsforholdSvar> =
            faktum.get("svar").values().map { eøsArbeidsforhold ->
                val arbeidsgivernavnSvar =
                    eøsArbeidsforhold
                        .values()
                        .find { it.get("beskrivendeId").asString() == "faktum.eos-arbeidsforhold.arbeidsgivernavn" }
                        ?.get("svar")
                        ?.asString() ?: ""

                val landSvar =
                    eøsArbeidsforhold
                        .values()
                        .find { it.get("beskrivendeId").asString() == "faktum.eos-arbeidsforhold.land" }
                        ?.get("svar")
                        ?.asString() ?: ""

                val personnummerSvar =
                    eøsArbeidsforhold
                        .values()
                        .find { it.get("beskrivendeId").asString() == "faktum.eos-arbeidsforhold.personnummer" }
                        ?.get("svar")
                        ?.asString() ?: ""

                val varighet =
                    eøsArbeidsforhold
                        .values()
                        .find { it.get("beskrivendeId").asString() == "faktum.eos-arbeidsforhold.varighet" }
                        ?.get("svar")

                val fom = varighet?.get("fom")?.asLocalDate() ?: throw IllegalArgumentException("Fom dato mangler")
                val tom = varighet.get("tom")?.asLocalDate()

                EøsArbeidsforholdSvar(
                    bedriftsnavn = arbeidsgivernavnSvar,
                    land = landSvar,
                    personnummerIArbeidsland = personnummerSvar,
                    varighet = PeriodeSvar(fom, tom),
                )
            }
        return QuizOpplysning(beskrivendeId, EøsArbeidsforhold, eøsArbeidsforholdSvar, ident, søknadId)
    }
}

data class EøsArbeidsforholdSvar(
    val bedriftsnavn: String,
    val land: String,
    val personnummerIArbeidsland: String,
    val varighet: PeriodeSvar,
)
