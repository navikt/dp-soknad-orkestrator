package no.nav.dagpenger.soknad.orkestrator.søknad

import com.fasterxml.jackson.databind.JsonNode
import no.nav.dagpenger.soknad.orkestrator.opplysning.Arbeidsforhold
import no.nav.dagpenger.soknad.orkestrator.opplysning.ArbeidsforholdSvar
import no.nav.dagpenger.soknad.orkestrator.opplysning.Boolsk
import no.nav.dagpenger.soknad.orkestrator.opplysning.Dato
import no.nav.dagpenger.soknad.orkestrator.opplysning.Desimaltall
import no.nav.dagpenger.soknad.orkestrator.opplysning.Flervalg
import no.nav.dagpenger.soknad.orkestrator.opplysning.Heltall
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysning
import no.nav.dagpenger.soknad.orkestrator.opplysning.Periode
import no.nav.dagpenger.soknad.orkestrator.opplysning.PeriodeSvar
import no.nav.dagpenger.soknad.orkestrator.opplysning.Tekst
import no.nav.dagpenger.soknad.orkestrator.utils.asUUID
import no.nav.helse.rapids_rivers.asLocalDate
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
        opplysninger.addAll(faktaTilOpplysninger(søknadData, ident, søknadId))

        Søknad(
            id = søknadId,
            ident = ident,
            opplysninger = opplysninger,
        )
    }

    private fun faktaTilOpplysninger(
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
                val type = faktum.get("type").asText()

                val opplysning =
                    when (type) {
                        "land", "tekst", "envalg" ->
                            Opplysning(
                                beskrivendeId = beskrivendeId,
                                type = Tekst,
                                svar = faktum.get("svar").asText(),
                                ident = ident,
                                søknadsId = søknadId,
                            )

                        "int" ->
                            Opplysning(
                                beskrivendeId = beskrivendeId,
                                type = Heltall,
                                svar = faktum.get("svar").asInt(),
                                ident = ident,
                                søknadsId = søknadId,
                            )

                        "double" ->
                            Opplysning(
                                beskrivendeId = beskrivendeId,
                                type = Desimaltall,
                                svar = faktum.get("svar").asDouble(),
                                ident = ident,
                                søknadsId = søknadId,
                            )

                        "boolean" ->
                            Opplysning(
                                beskrivendeId = beskrivendeId,
                                type = Boolsk,
                                svar = faktum.get("svar").asBoolean(),
                                ident = ident,
                                søknadsId = søknadId,
                            )

                        "localdate" ->
                            Opplysning(
                                beskrivendeId = beskrivendeId,
                                type = Dato,
                                svar = faktum.get("svar").asLocalDate(),
                                ident = ident,
                                søknadsId = søknadId,
                            )

                        "flervalg" ->
                            Opplysning(
                                beskrivendeId = beskrivendeId,
                                type = Flervalg,
                                svar = faktum.get("svar").map { it.asText() },
                                ident = ident,
                                søknadsId = søknadId,
                            )

                        "periode" ->
                            Opplysning(
                                beskrivendeId,
                                Periode,
                                periodeSvar(faktum.get("svar")),
                                ident,
                                søknadId,
                            )

                        "generator" -> generatorSvar(faktum, ident, søknadId)

                        else -> throw IllegalArgumentException("Ukjent faktumtype: $type")
                    }
                opplysning
            }
        }.flatten()
    }
}

private fun periodeSvar(periodesvar: JsonNode?): PeriodeSvar {
    return periodesvar?.let {
        PeriodeSvar(
            fom = periodesvar.get("fom").asLocalDate(),
            tom = periodesvar.get("tom")?.asLocalDate(),
        )
    } ?: throw IllegalArgumentException("Mangler 'svar' i periodefaktum")
}

private fun generatorSvar(
    faktum: JsonNode,
    ident: String,
    søknadId: UUID,
): Opplysning<*> {
    val beskrivendeId = faktum.get("beskrivendeId").asText()
    return when (beskrivendeId) {
        "faktum.arbeidsforhold" -> {
            val arbeidsforholdSvar: List<ArbeidsforholdSvar> =
                faktum.get("svar").map { arbeidsforhold ->
                    val arbeidsforholdMap = arbeidsforhold.associateBy { it.get("beskrivendeId").asText() }

                    // TODO: Håndter null på en bedre måte
                    ArbeidsforholdSvar(
                        navn = arbeidsforholdMap["faktum.arbeidsforhold.navn-bedrift"]?.get("svar")?.asText() ?: "",
                        land = arbeidsforholdMap["faktum.arbeidsforhold.land"]?.get("svar")?.asText() ?: "",
                    )
                }
            Opplysning(beskrivendeId, Arbeidsforhold, arbeidsforholdSvar, ident, søknadId)
        }

        "faktum.eos-arbeidsforhold" -> Opplysning(beskrivendeId, Tekst, "EOS TODO", ident, søknadId)
        "faktum.egen-naering-organisasjonsnummer-liste" ->
            Opplysning(
                beskrivendeId,
                Tekst,
                "EGEN NÆRING TODO",
                ident,
                søknadId,
            )

        "faktum.register.barn-liste" -> Opplysning(beskrivendeId, Tekst, "PDL BARN TODO", ident, søknadId)
        "faktum.barn-liste" -> Opplysning(beskrivendeId, Tekst, "BARN TODO", ident, søknadId)

        else -> throw IllegalArgumentException("Ukjent generatorfaktum: $beskrivendeId")
    }
}
