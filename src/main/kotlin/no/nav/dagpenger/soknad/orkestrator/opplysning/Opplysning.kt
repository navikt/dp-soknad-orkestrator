package no.nav.dagpenger.soknad.orkestrator.opplysning

import java.time.LocalDate
import java.util.UUID

data class Opplysning<T>(
    val beskrivendeId: String,
    val type: Datatype<T>,
    val svar: T,
    val ident: String,
    val søknadsId: UUID,
)

sealed class Datatype<T>(val klasse: Class<T>)

data object Tekst : Datatype<String>(String::class.java)

data object Heltall : Datatype<Int>(Int::class.java)

data object Desimaltall : Datatype<Double>(Double::class.java)

data object Boolsk : Datatype<Boolean>(Boolean::class.java)

data object Dato : Datatype<LocalDate>(LocalDate::class.java)

@Suppress("UNCHECKED_CAST")
data object Flervalg : Datatype<List<String>>(String::class.java as Class<List<String>>)

data object Periode : Datatype<PeriodeSvar>(PeriodeSvar::class.java)

data class PeriodeSvar(
    val fom: LocalDate,
    val tom: LocalDate?,
)

@Suppress("UNCHECKED_CAST")
data object Arbeidsforhold : Datatype<List<ArbeidsforholdSvar>>(
    List::class.java as Class<List<ArbeidsforholdSvar>>,
)

data class ArbeidsforholdSvar(
    val navn: String,
    val land: String,
)

@Suppress("UNCHECKED_CAST")
data object EøsArbeidsforhold : Datatype<List<EøsArbeidsforholdSvar>>(
    List::class.java as Class<List<EøsArbeidsforholdSvar>>,
)

data class EøsArbeidsforholdSvar(
    val bedriftnavn: String,
    val land: String,
    val personnummerIArbeidsland: String,
    val varighet: PeriodeSvar,
)
