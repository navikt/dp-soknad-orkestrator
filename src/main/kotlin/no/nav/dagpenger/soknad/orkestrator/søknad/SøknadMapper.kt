package no.nav.dagpenger.soknad.orkestrator.søknad

import com.fasterxml.jackson.databind.JsonNode
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
        val søknadstidspunkt = jsonNode.get("søknadstidspunkt").asText()
        val søknadData = jsonNode.get("søknadData")

        val opplysninger = mutableListOf<Opplysning<*>>()

        val søknadstidspunktOpplysning =
            Opplysning(
                beskrivendeId = "søknadstidspunkt",
                type = Tekst,
                svar = søknadstidspunkt,
                ident = ident,
                søknadsId = søknadId,
            )

        opplysninger.add(søknadstidspunktOpplysning)
        opplysninger.addAll(søknadDataTilOpplysninger(søknadData, ident, søknadId))

        Søknad(
            id = søknadId,
            ident = ident,
            opplysninger = opplysninger,
        )
    }

    private fun søknadDataTilOpplysninger(
        søknadData: JsonNode,
        ident: String,
        søknadId: UUID,
    ): List<Opplysning<*>> {
        val seksjoner =
            søknadData["seksjoner"]
                ?: throw IllegalArgumentException("Mangler seksjoner")

        return seksjoner.asIterable().map { seksjon ->
            val fakta = seksjon.get("fakta") ?: throw IllegalArgumentException("Mangler fakta")
            fakta.asIterable().map { faktum ->
                val beskrivendeId = faktum.get("beskrivendeId").asText()
                val faktumtype = faktum.get("type").asText()

                val datatype: Datatype<*> = finnDatatype(faktumtype, beskrivendeId)
                datatype.tilOpplysning(faktum, beskrivendeId, ident, søknadId)
            }
        }.flatten()
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
                    "faktum.register.barn-liste" -> datatyper.getValue("barn")
                    "faktum.barn-liste" -> datatyper.getValue("barn")
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
