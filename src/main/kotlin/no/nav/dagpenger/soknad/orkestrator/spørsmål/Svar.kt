package no.nav.dagpenger.soknad.orkestrator.spørsmål

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.PeriodeSvar
import java.time.LocalDate
import java.util.UUID

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = LandSvar::class, name = "LAND"),
    JsonSubTypes.Type(value = BooleanSvar::class, name = "BOOLEAN"),
    JsonSubTypes.Type(value = DatoSvar::class, name = "DATO"),
    JsonSubTypes.Type(value = TekstSvar::class, name = "TEKST"),
    JsonSubTypes.Type(value = PeriodesvarSvar::class, name = "PERIODE"),
)
abstract class Svar<T>(
    val spørsmålId: UUID,
    val type: SpørsmålType,
    var verdi: T,
)

class LandSvar(spørsmålId: UUID, verdi: String) : Svar<String>(
    spørsmålId = spørsmålId,
    type = SpørsmålType.LAND,
    verdi = verdi,
) {
    init {
        require(verdi.length == 3) {
            "ISO 3166-1-alpha3 må være 3 bokstaver lang. Fikk: $verdi"
        }
    }
}

class BooleanSvar(spørsmålId: UUID, verdi: Boolean) : Svar<Boolean>(
    spørsmålId = spørsmålId,
    type = SpørsmålType.BOOLEAN,
    verdi = verdi,
)

class DatoSvar(spørsmålId: UUID, verdi: LocalDate) : Svar<LocalDate>(
    spørsmålId = spørsmålId,
    type = SpørsmålType.DATO,
    verdi = verdi,
)

class TekstSvar(spørsmålId: UUID, verdi: String) : Svar<String>(
    spørsmålId = spørsmålId,
    type = SpørsmålType.TEKST,
    verdi = verdi,
)

class PeriodesvarSvar(spørsmålId: UUID, verdi: PeriodeSvar) : Svar<PeriodeSvar>(
    spørsmålId = spørsmålId,
    type = SpørsmålType.PERIODE,
    verdi = verdi,
)
