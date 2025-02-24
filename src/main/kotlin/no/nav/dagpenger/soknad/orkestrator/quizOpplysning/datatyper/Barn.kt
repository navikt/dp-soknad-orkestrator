package no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.QuizOpplysning
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

                // Vi antar at barnet kvalifiserer til barnetillegg hvis brukeren forsørger barnet
                val kvalifisererTilBarnetillegg = forsørgerBarnet
                val barnetilleggperiode = if (kvalifisererTilBarnetillegg) barnetilleggperiode(fødselsdato) else null

                BarnSvar(
                    barnSvarId = UUID.randomUUID(),
                    fornavnOgMellomnavn = fornavnOgMellomnavn,
                    etternavn = etternavn,
                    fødselsdato = fødselsdato,
                    statsborgerskap = statsborgerskap,
                    forsørgerBarnet = forsørgerBarnet,
                    fraRegister = fraRegister,
                    kvalifisererTilBarnetillegg = kvalifisererTilBarnetillegg,
                    barnetilleggFom = barnetilleggperiode?.first,
                    barnetilleggTom = barnetilleggperiode?.second,
                )
            }

        return QuizOpplysning(beskrivendeId, Barn, barnSvar, ident, søknadId)
    }

    internal fun barnetilleggperiode(fødselsdato: LocalDate): Pair<LocalDate, LocalDate> = fødselsdato to fødselsdato.plusYears(18)
}

data class BarnSvar(
    val barnSvarId: UUID,
    val fornavnOgMellomnavn: String,
    val etternavn: String,
    val fødselsdato: LocalDate,
    val statsborgerskap: String,
    val forsørgerBarnet: Boolean,
    val fraRegister: Boolean,
    val kvalifisererTilBarnetillegg: Boolean,
    val barnetilleggFom: LocalDate? = null,
    val barnetilleggTom: LocalDate? = null,
    val endretAv: String? = null,
    val begrunnelse: String? = null,
)
