package no.nav.dagpenger.soknad.orkestrator.spørsmål

import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.PeriodeSvar
import java.time.LocalDate
import java.util.UUID

abstract class Spørsmål<T>(
    val id: UUID = UUID.randomUUID(),
    val tekstnøkkel: String,
    val type: SpørsmålType,
    var svar: T? = null,
    val gyldigeSvar: List<String>? = null,
)

class LandSpørsmål(
    tekstnøkkel: String,
    gyldigeSvar: List<String>,
) : Spørsmål<String>(
        tekstnøkkel = tekstnøkkel,
        gyldigeSvar = gyldigeSvar,
        type = SpørsmålType.LAND,
    )

class BooleanSpørsmål(
    tekstnøkkel: String,
) : Spørsmål<Boolean>(
        tekstnøkkel = tekstnøkkel,
        type = SpørsmålType.BOOLEAN,
    )

class DatoSpørsmål(
    tekstnøkkel: String,
) : Spørsmål<LocalDate>(
        tekstnøkkel = tekstnøkkel,
        type = SpørsmålType.DATO,
    )

class TekstSpørsmål(
    tekstnøkkel: String,
) : Spørsmål<String>(
        tekstnøkkel = tekstnøkkel,
        type = SpørsmålType.TEKST,
    )

class PeriodeSpørsmål(
    tekstnøkkel: String,
) : Spørsmål<PeriodeSvar>(
        tekstnøkkel = tekstnøkkel,
        type = SpørsmålType.PERIODE,
    )
