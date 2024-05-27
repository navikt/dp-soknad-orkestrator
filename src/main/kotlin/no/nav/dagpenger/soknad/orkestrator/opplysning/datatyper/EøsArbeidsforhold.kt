package no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper

import com.fasterxml.jackson.databind.JsonNode
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysning
import no.nav.helse.rapids_rivers.asLocalDate
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
    ): Opplysning<*> {
        val eøsArbeidsforholdSvar: List<EøsArbeidsforholdSvar> =
            faktum.get("svar").map { eøsArbeidsforhold ->
                val arbeidsgivernavnSvar =
                    eøsArbeidsforhold
                        .find { it.get("beskrivendeId").asText() == "faktum.eos-arbeidsforhold.arbeidsgivernavn" }
                        ?.get("svar")?.asText() ?: ""

                val landSvar =
                    eøsArbeidsforhold
                        .find { it.get("beskrivendeId").asText() == "faktum.eos-arbeidsforhold.land" }
                        ?.get("svar")?.asText() ?: ""

                val personnummerSvar =
                    eøsArbeidsforhold
                        .find { it.get("beskrivendeId").asText() == "faktum.eos-arbeidsforhold.personnummer" }
                        ?.get("svar")?.asText() ?: ""

                val varighet =
                    eøsArbeidsforhold
                        .find { it.get("beskrivendeId").asText() == "faktum.eos-arbeidsforhold.varighet" }
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
        return Opplysning(beskrivendeId, EøsArbeidsforhold, eøsArbeidsforholdSvar, ident, søknadId)
    }
}

data class EøsArbeidsforholdSvar(
    val bedriftsnavn: String,
    val land: String,
    val personnummerIArbeidsland: String,
    val varighet: PeriodeSvar,
)
