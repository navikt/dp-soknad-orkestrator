package no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper

import com.fasterxml.jackson.databind.JsonNode
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysning
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
    ): Opplysning<*> {
        val arbeidsforholdSvar: List<ArbeidsforholdSvar> =
            faktum.get("svar").map { arbeidsforhold ->
                // TODO: Hvordan vil vi håndtere null her?
                val navnSvar =
                    arbeidsforhold
                        .find { it.get("beskrivendeId").asText() == "faktum.arbeidsforhold.navn-bedrift" }
                        ?.get("svar")?.asText() ?: ""

                val landFaktum =
                    arbeidsforhold
                        .find { it.get("beskrivendeId").asText() == "faktum.arbeidsforhold.land" }
                        ?.get("svar")?.asText() ?: ""

                ArbeidsforholdSvar(
                    navn = navnSvar,
                    land = landFaktum,
                )
            }
        return Opplysning(beskrivendeId, Arbeidsforhold, arbeidsforholdSvar, ident, søknadId)
    }
}

data class ArbeidsforholdSvar(
    val navn: String,
    val land: String,
)
