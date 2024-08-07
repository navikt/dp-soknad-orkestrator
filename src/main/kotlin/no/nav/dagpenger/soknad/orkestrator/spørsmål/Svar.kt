package no.nav.dagpenger.soknad.orkestrator.spørsmål

import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.PeriodeSvar
import java.time.LocalDate
import java.util.UUID

abstract class Svar<T>(
    val id: UUID = UUID.randomUUID(),
    val type: SpørsmålType,
    var verdi: T? = null,
    val spørsmålId: UUID,
)

class LandSvar(spørsmålId: UUID, verdi: String) : Svar<String>(
    type = SpørsmålType.LAND,
    verdi = verdi,
    spørsmålId = spørsmålId,
)

class BooleanSvar(spørsmålId: UUID, verdi: Boolean) : Svar<Boolean>(
    type = SpørsmålType.BOOLEAN,
    verdi = verdi,
    spørsmålId = spørsmålId,
)

class DatoSvar(spørsmålId: UUID, verdi: LocalDate) : Svar<LocalDate>(
    type = SpørsmålType.DATO,
    verdi = verdi,
    spørsmålId = spørsmålId,
)

class TekstSvar(spørsmålId: UUID, verdi: String) : Svar<String>(
    type = SpørsmålType.TEKST,
    verdi = verdi,
    spørsmålId = spørsmålId,
)

class PeriodesvarSvar(spørsmålId: UUID, verdi: PeriodeSvar) : Svar<PeriodeSvar>(
    type = SpørsmålType.PERIODE,
    verdi = verdi,
    spørsmålId = spørsmålId,
)
