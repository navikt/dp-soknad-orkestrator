package no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper

import com.fasterxml.jackson.databind.JsonNode
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.QuizOpplysning
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Sluttårsak.ARBEIDSGIVER_KONKURS
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Sluttårsak.AVSKJEDIGET
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Sluttårsak.IKKE_ENDRET
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Sluttårsak.KONTRAKT_UTGAATT
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Sluttårsak.REDUSERT_ARBEIDSTID
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Sluttårsak.SAGT_OPP_AV_ARBEIDSGIVER
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Sluttårsak.SAGT_OPP_SELV
import java.util.UUID

@Suppress("UNCHECKED_CAST")
data object Arbeidsforhold : Datatype<List<ArbeidsforholdSvar>>(
    List::class.java as Class<List<ArbeidsforholdSvar>>,
) {
    override fun tilOpplysning(
        faktum: JsonNode,
        beskrivendeId: String,
        ident: String,
        søknadId: UUID,
    ): QuizOpplysning<*>? {
        val kompletteArbeidsforhold = faktum["svar"].filter { harAllePåkrevdeFelt(it) }

        if (kompletteArbeidsforhold.isEmpty()) return null

        val arbeidsforholdSvar: List<ArbeidsforholdSvar> = hentArbeidsforholdSvar(kompletteArbeidsforhold)

        return QuizOpplysning(beskrivendeId, Arbeidsforhold, arbeidsforholdSvar, ident, søknadId)
    }

    private fun harAllePåkrevdeFelt(arbeidsforhold: JsonNode): Boolean {
        val påkrevdeFelter =
            mutableListOf(
                "faktum.arbeidsforhold.navn-bedrift",
                "faktum.arbeidsforhold.land",
                "faktum.arbeidsforhold.endret",
            )

        if (erPermittering(arbeidsforhold)) {
            påkrevdeFelter += "faktum.arbeidsforhold.permittertert-fra-fiskeri-naering"
        }

        return påkrevdeFelter.all { påkrevdFelt -> arbeidsforhold.harPåkrevdFelt(påkrevdFelt) }
    }

    private fun erPermittering(arbeidsforhold: JsonNode) =
        arbeidsforhold.any { it["svar"].asText() == "faktum.arbeidsforhold.endret.svar.permittert" }

    private fun JsonNode.harPåkrevdFelt(påkrevdFelt: String): Boolean {
        return this.any { it["beskrivendeId"].asText() == påkrevdFelt }
    }

    private fun hentArbeidsforholdSvar(kompletteArbeidsforhold: List<JsonNode>) =
        kompletteArbeidsforhold.map { arbeidsforhold ->
            val navnSvar =
                arbeidsforhold
                    .single { it.get("beskrivendeId").asText() == "faktum.arbeidsforhold.navn-bedrift" }
                    .get("svar").asText()

            val landFaktum =
                arbeidsforhold
                    .single { it.get("beskrivendeId").asText() == "faktum.arbeidsforhold.land" }
                    .get("svar").asText()

            val sluttårsak = finnSluttårsak(arbeidsforhold)

            ArbeidsforholdSvar(
                navn = navnSvar,
                land = landFaktum,
                sluttårsak = sluttårsak,
            )
        }

    internal fun finnSluttårsak(arbeidsforhold: JsonNode): Sluttårsak {
        return arbeidsforhold.single { it.get("beskrivendeId").asText() == "faktum.arbeidsforhold.endret" }
            .get("svar").asText().let {
                when (it) {
                    "faktum.arbeidsforhold.endret.svar.arbeidsgiver-konkurs" -> ARBEIDSGIVER_KONKURS
                    "faktum.arbeidsforhold.endret.svar.permittert" -> finnSluttårsakVedPermittering(arbeidsforhold)
                    "faktum.arbeidsforhold.endret.svar.avskjediget" -> AVSKJEDIGET
                    "faktum.arbeidsforhold.endret.svar.kontrakt-utgaatt" -> KONTRAKT_UTGAATT
                    "faktum.arbeidsforhold.endret.svar.redusert-arbeidstid" -> REDUSERT_ARBEIDSTID
                    "faktum.arbeidsforhold.endret.svar.sagt-opp-av-arbeidsgiver" -> SAGT_OPP_AV_ARBEIDSGIVER
                    "faktum.arbeidsforhold.endret.svar.sagt-opp-selv" -> SAGT_OPP_SELV
                    "faktum.arbeidsforhold.endret.svar.ikke-endret" -> IKKE_ENDRET
                    else -> throw IllegalArgumentException("Ukjent sluttårsak: $it")
                }
            }
    }

    private fun finnSluttårsakVedPermittering(arbeidsforhold: JsonNode): Sluttårsak {
        val permittertFraFiskeriFaktum =
            arbeidsforhold.single {
                it.get("beskrivendeId").asText() == "faktum.arbeidsforhold.permittertert-fra-fiskeri-naering"
            }

        return when (permittertFraFiskeriFaktum.get("svar").asBoolean()) {
            true -> Sluttårsak.PERMITTERT_FISKEFOREDLING
            else -> Sluttårsak.PERMITTERT
        }
    }
}

data class ArbeidsforholdSvar(
    val navn: String,
    val land: String,
    val sluttårsak: Sluttårsak,
)

enum class Sluttårsak {
    ARBEIDSGIVER_KONKURS,
    PERMITTERT,
    PERMITTERT_FISKEFOREDLING,
    AVSKJEDIGET,
    KONTRAKT_UTGAATT,
    REDUSERT_ARBEIDSTID,
    SAGT_OPP_AV_ARBEIDSGIVER,
    SAGT_OPP_SELV,
    IKKE_ENDRET,
}
