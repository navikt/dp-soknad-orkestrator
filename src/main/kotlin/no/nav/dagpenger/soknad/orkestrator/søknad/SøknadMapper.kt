package no.nav.dagpenger.soknad.orkestrator.søknad

import com.fasterxml.jackson.databind.JsonNode
import mu.KotlinLogging
import no.nav.dagpenger.soknad.orkestrator.metrikker.SøknadMetrikker
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.QuizOpplysning
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Arbeidsforhold
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Barn
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Boolsk
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Datatype
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Dato
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Desimaltall
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.EgenNæring
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.EøsArbeidsforhold
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Flervalg
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Heltall
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Periode
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Tekst
import no.nav.dagpenger.soknad.orkestrator.utils.asUUID
import java.util.UUID

class SøknadMapper(
    private val jsonNode: JsonNode,
) {
    val søknad by lazy {
        val ident = jsonNode.get("ident").asText()
        val søknadId = jsonNode.get("søknadId").asUUID()
        val søknadData = jsonNode.get("søknadData")
        val søknadstidspunkt = jsonNode.get("søknadstidspunkt").asText()

        Søknad(
            søknadId = søknadId,
            ident = ident,
            tilstand = Tilstand.INNSENDT,
            opplysninger = søknadDataTilOpplysninger(søknadData, ident, søknadId, søknadstidspunkt),
        )
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
    }

    private fun søknadDataTilOpplysninger(
        søknadData: JsonNode,
        ident: String,
        søknadId: UUID,
        søknadstidspunkt: String,
    ): List<QuizOpplysning<*>> {
        try {
            val seksjoner = søknadData["seksjoner"]

            val søknadstidspunktOpplysning =
                QuizOpplysning(
                    beskrivendeId = "søknadstidspunkt",
                    type = Tekst,
                    svar = søknadstidspunkt,
                    ident = ident,
                    søknadId = søknadId,
                )

            val opplysninger =
                seksjoner.asIterable().flatMap { seksjon ->
                    val fakta = seksjon.get("fakta")
                    fakta.asIterable().mapNotNull { faktum ->
                        opprettOpplysning(faktum, ident, søknadId)
                    }
                } + søknadstidspunktOpplysning

            SøknadMetrikker.dekomponert.inc()
            return opplysninger
        } catch (e: Exception) {
            logger.warn { "Feil ved mapping av søknaddata til opplysninger: ${e.message}" }
            SøknadMetrikker.dekomponeringFeilet.inc()
            throw IllegalArgumentException("Kunne ikke mappe søknad $søknadId til opplysninger", e)
        }
    }

    private fun opprettOpplysning(
        faktum: JsonNode,
        ident: String,
        søknadId: UUID,
    ): QuizOpplysning<*>? {
        val beskrivendeId = faktum.get("beskrivendeId").asText()
        val faktumtype = faktum.get("type").asText()

        if (faktumtype == "dokument") {
            return null
        }

        val datatype: Datatype<*> = finnDatatype(faktumtype, beskrivendeId)
        return datatype.tilOpplysning(faktum, beskrivendeId, ident, søknadId)
    }

    private fun finnDatatype(
        type: String,
        beskrivendeId: String?,
    ): Datatype<*> {
        val datatype: Datatype<*> =
            if (type == "generator") {
                when (beskrivendeId) {
                    "faktum.arbeidsforhold" -> datatyper.getValue("arbeidsforhold")
                    "faktum.eos-arbeidsforhold" -> datatyper.getValue("eøsArbeidsforhold")
                    "faktum.egen-naering-organisasjonsnummer-liste" -> datatyper.getValue("egenNæring")
                    "faktum.register.barn-liste", "faktum.barn-liste" -> datatyper.getValue("barn")
                    else -> throw IllegalArgumentException("Ukjent generator")
                }
            } else {
                datatyper.getValue(type)
            }
        return datatype
    }

    private val datatyper: Map<String, Datatype<*>> =
        mapOf(
            "tekst" to Tekst,
            "land" to Tekst,
            "envalg" to Tekst,
            "int" to Heltall,
            "double" to Desimaltall,
            "boolean" to Boolsk,
            "localdate" to Dato,
            "flervalg" to Flervalg,
            "periode" to Periode,
            "arbeidsforhold" to Arbeidsforhold,
            "eøsArbeidsforhold" to EøsArbeidsforhold,
            "egenNæring" to EgenNæring,
            "barn" to Barn,
        )
}
