package no.nav.dagpenger.soknad.orkestrator.opplysning

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.asLocalDate
import java.time.LocalDate
import java.util.UUID

data class Opplysning<T>(
    val beskrivendeId: String,
    val type: Datatype<T>,
    val svar: T,
    val ident: String,
    val søknadsId: UUID,
)

sealed class Datatype<T>(val klasse: Class<T>) {
    abstract fun tilOpplysning(
        faktum: JsonNode,
        beskrivendeId: String,
        ident: String,
        søknadId: UUID,
    ): Opplysning<*>
}

data object Tekst : Datatype<String>(String::class.java) {
    override fun tilOpplysning(
        faktum: JsonNode,
        beskrivendeId: String,
        ident: String,
        søknadId: UUID,
    ): Opplysning<*> {
        val svar = faktum.get("svar").asText()
        return Opplysning(beskrivendeId, Tekst, svar, ident, søknadId)
    }
}

data object Heltall : Datatype<Int>(Int::class.java) {
    override fun tilOpplysning(
        faktum: JsonNode,
        beskrivendeId: String,
        ident: String,
        søknadId: UUID,
    ): Opplysning<*> {
        val svar = faktum.get("svar").asInt()
        return Opplysning(beskrivendeId, Heltall, svar, ident, søknadId)
    }
}

data object Desimaltall : Datatype<Double>(Double::class.java) {
    override fun tilOpplysning(
        faktum: JsonNode,
        beskrivendeId: String,
        ident: String,
        søknadId: UUID,
    ): Opplysning<*> {
        val svar = faktum.get("svar").asDouble()
        return Opplysning(beskrivendeId, Desimaltall, svar, ident, søknadId)
    }
}

data object Boolsk : Datatype<Boolean>(Boolean::class.java) {
    override fun tilOpplysning(
        faktum: JsonNode,
        beskrivendeId: String,
        ident: String,
        søknadId: UUID,
    ): Opplysning<*> {
        val svar = faktum.get("svar").asBoolean()
        return Opplysning(beskrivendeId, Boolsk, svar, ident, søknadId)
    }
}

data object Dato : Datatype<LocalDate>(LocalDate::class.java) {
    override fun tilOpplysning(
        faktum: JsonNode,
        beskrivendeId: String,
        ident: String,
        søknadId: UUID,
    ): Opplysning<*> {
        val svar = faktum.get("svar").asLocalDate()
        return Opplysning(beskrivendeId, Dato, svar, ident, søknadId)
    }
}

@Suppress("UNCHECKED_CAST")
data object Flervalg : Datatype<List<String>>(String::class.java as Class<List<String>>) {
    override fun tilOpplysning(
        faktum: JsonNode,
        beskrivendeId: String,
        ident: String,
        søknadId: UUID,
    ): Opplysning<*> {
        val svar = faktum.get("svar").map { it.asText() }
        return Opplysning(beskrivendeId, Flervalg, svar, ident, søknadId)
    }
}

data object Periode : Datatype<PeriodeSvar>(PeriodeSvar::class.java) {
    override fun tilOpplysning(
        faktum: JsonNode,
        beskrivendeId: String,
        ident: String,
        søknadId: UUID,
    ): Opplysning<*> {
        val svar =
            PeriodeSvar(
                fom = faktum.get("svar").get("fom").asLocalDate(),
                tom = faktum.get("svar").get("tom")?.asLocalDate(),
            )
        return Opplysning(beskrivendeId, Periode, svar, ident, søknadId)
    }
}

data class PeriodeSvar(
    val fom: LocalDate,
    val tom: LocalDate?,
)

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
                    bedriftnavn = arbeidsgivernavnSvar,
                    land = landSvar,
                    personnummerIArbeidsland = personnummerSvar,
                    varighet = PeriodeSvar(fom, tom),
                )
            }
        return Opplysning(beskrivendeId, EøsArbeidsforhold, eøsArbeidsforholdSvar, ident, søknadId)
    }
}

data class EøsArbeidsforholdSvar(
    val bedriftnavn: String,
    val land: String,
    val personnummerIArbeidsland: String,
    val varighet: PeriodeSvar,
)
