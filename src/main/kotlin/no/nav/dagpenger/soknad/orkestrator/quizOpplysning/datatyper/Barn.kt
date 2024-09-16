package no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper

import com.fasterxml.jackson.databind.JsonNode
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.QuizOpplysning
import no.nav.helse.rapids_rivers.asLocalDate
import java.time.LocalDate
import java.util.UUID

@Suppress("UNCHECKED_CAST")
data object Barn : Datatype<List<BarnSvar>>(List::class.java as Class<List<BarnSvar>>) {
    override fun tilOpplysning(
        faktum: JsonNode,
        beskrivendeId: String,
        ident: String,
        søknadId: UUID,
    ): QuizOpplysning<*> {
        val fraRegister = beskrivendeId == "faktum.register.barn-liste"

        val barnSvar: List<BarnSvar> =
            faktum.get("svar").map { it ->
                val fornavnOgMellomnavn =
                    it.single { it.get("beskrivendeId").asText() == "faktum.barn-fornavn-mellomnavn" }
                        .get("svar").asText()
                val etternavn =
                    it.single { it.get("beskrivendeId").asText() == "faktum.barn-etternavn" }.get("svar")
                        .asText()
                val fødselsdato =
                    it.single { it.get("beskrivendeId").asText() == "faktum.barn-foedselsdato" }.get("svar")
                        .asLocalDate()
                val statsborgerskap =
                    it.single { it.get("beskrivendeId").asText() == "faktum.barn-statsborgerskap" }
                        .get("svar").asText()
                val forsørgerBarnet =
                    it.single { it.get("beskrivendeId").asText() == "faktum.forsoerger-du-barnet" }
                        .get("svar").asBoolean()

                BarnSvar(
                    fornavnOgMellomnavn,
                    etternavn,
                    fødselsdato,
                    statsborgerskap,
                    forsørgerBarnet,
                    fraRegister,
                )
            }

        return QuizOpplysning(beskrivendeId, Barn, barnSvar, ident, søknadId)
    }
}

data class BarnSvar(
    val fornavnOgMellomnavn: String,
    val etternavn: String,
    val fødselsdato: LocalDate,
    val statsborgerskap: String,
    val forsørgerBarnet: Boolean,
    val fraRegister: Boolean,
)
