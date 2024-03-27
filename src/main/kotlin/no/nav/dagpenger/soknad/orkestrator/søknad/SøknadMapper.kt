package no.nav.dagpenger.soknad.orkestrator.søknad

import com.fasterxml.jackson.databind.JsonNode
import no.nav.dagpenger.soknad.orkestrator.opplysning.Arbeidsforhold
import no.nav.dagpenger.soknad.orkestrator.opplysning.ArbeidsforholdSvar
import no.nav.dagpenger.soknad.orkestrator.opplysning.Boolsk
import no.nav.dagpenger.soknad.orkestrator.opplysning.Dato
import no.nav.dagpenger.soknad.orkestrator.opplysning.Desimaltall
import no.nav.dagpenger.soknad.orkestrator.opplysning.EøsArbeidsforhold
import no.nav.dagpenger.soknad.orkestrator.opplysning.EøsArbeidsforholdSvar
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
        "faktum.arbeidsforhold" -> tilArbeidsforholdOpplysning(faktum, beskrivendeId, ident, søknadId)

        "faktum.eos-arbeidsforhold" -> tilEøsArbeidsforholdOpplysning(faktum, beskrivendeId, ident, søknadId)

        // TODO: Håndter resten av generatorfaktumene
        // "faktum.egen-naering-organisasjonsnummer-liste" ->
        // "faktum.register.barn-liste" ->
        // "faktum.barn-liste" ->

        else -> throw IllegalArgumentException("Ukjent generatorfaktum: $beskrivendeId")
    }
}

private fun tilArbeidsforholdOpplysning(
    faktum: JsonNode,
    beskrivendeId: String,
    ident: String,
    søknadId: UUID,
): Opplysning<List<ArbeidsforholdSvar>> {
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

fun tilEøsArbeidsforholdOpplysning(
    faktum: JsonNode,
    beskrivendeId: String,
    ident: String,
    søknadId: UUID,
): Opplysning<List<EøsArbeidsforholdSvar>> {
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

            val varighetSvar =
                eøsArbeidsforhold
                    .find { it.get("beskrivendeId").asText() == "faktum.eos-arbeidsforhold.varighet" }
                    ?.get("svar")

            EøsArbeidsforholdSvar(
                bedriftnavn = arbeidsgivernavnSvar,
                land = landSvar,
                personnummerIArbeidsland = personnummerSvar,
                varighet = periodeSvar(varighetSvar),
            )
        }
    return Opplysning(beskrivendeId, EøsArbeidsforhold, eøsArbeidsforholdSvar, ident, søknadId)
}
