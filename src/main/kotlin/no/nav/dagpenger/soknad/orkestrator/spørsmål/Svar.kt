package no.nav.dagpenger.soknad.orkestrator.spørsmål

import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.PeriodeSvar
import java.time.LocalDate
import java.util.UUID

abstract class Svar<T>(
    val id: UUID = UUID.randomUUID(),
    val type: SpørsmålType,
    var verdi: T? = null,
)

class LandSvar(verdi: String) : Svar<String>(
    type = SpørsmålType.LAND,
    verdi = verdi,
)

class BooleanSvar(verdi: Boolean) : Svar<Boolean>(
    type = SpørsmålType.BOOLEAN,
    verdi = verdi,
)

class DatoSvar(verdi: LocalDate) : Svar<LocalDate>(
    type = SpørsmålType.DATO,
    verdi = verdi,
)

class TekstSvar(verdi: String) : Svar<String>(
    type = SpørsmålType.TEKST,
    verdi = verdi,
)

class PeriodesvarSvar(verdi: PeriodeSvar) : Svar<PeriodeSvar>(
    type = SpørsmålType.PERIODE,
    verdi = verdi,
)
