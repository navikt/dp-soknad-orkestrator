package no.nav.dagpenger.soknad.orkestrator.søknad

import com.fasterxml.jackson.databind.JsonNode
import mu.KotlinLogging
import no.nav.dagpenger.soknad.orkestrator.metrikker.SøknadMetrikker
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysning
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.Arbeidsforhold
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.Barn
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.Boolsk
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.Datatype
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.Dato
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.Desimaltall
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.EgenNæring
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.EøsArbeidsforhold
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.Flervalg
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.Heltall
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.Periode
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.Tekst
import no.nav.dagpenger.soknad.orkestrator.utils.asUUID
import java.util.UUID

class SøknadMapper(private val jsonNode: JsonNode) {
    val søknad by lazy {
        val ident = jsonNode.get("ident").asText()
        val søknadId = jsonNode.get("søknadId").asUUID()
        val søknadData = jsonNode.get("søknadData")

        Søknad(
            søknadId = søknadId,
            ident = ident,
            opplysninger = søknadDataTilOpplysninger(søknadData, ident, søknadId),
        )
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
    }

    private fun søknadDataTilOpplysninger(
        søknadData: JsonNode,
        ident: String,
        søknadId: UUID,
    ): List<Opplysning<*>> {
        try {
            val seksjoner = søknadData["seksjoner"]

            val opplysninger =
                seksjoner.asIterable().flatMap { seksjon ->
                    val fakta = seksjon.get("fakta")
                    fakta.asIterable().mapNotNull { faktum ->
                        opprettOpplysning(faktum, ident, søknadId)
                    }
                }

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
    ): Opplysning<*>? {
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
