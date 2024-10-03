package no.nav.dagpenger.soknad.orkestrator.opplysning

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.PeriodeSvar
import java.time.LocalDate
import java.util.UUID

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = LandSvar::class, name = "LAND"),
    JsonSubTypes.Type(value = LandSvar::class, name = "land"),
    JsonSubTypes.Type(value = BooleanSvar::class, name = "BOOLEAN"),
    JsonSubTypes.Type(value = BooleanSvar::class, name = "boolean"),
    JsonSubTypes.Type(value = DatoSvar::class, name = "DATO"),
    JsonSubTypes.Type(value = DatoSvar::class, name = "dato"),
    JsonSubTypes.Type(value = TekstSvar::class, name = "TEKST"),
    JsonSubTypes.Type(value = TekstSvar::class, name = "tekst"),
    JsonSubTypes.Type(value = PeriodesvarSvar::class, name = "PERIODE"),
    JsonSubTypes.Type(value = PeriodesvarSvar::class, name = "periode"),
)
abstract class Svar<T>(
    val opplysningId: UUID,
    val type: Opplysningstype,
    var verdi: T,
)

class LandSvar(opplysningId: UUID, verdi: String) : Svar<String>(
    opplysningId = opplysningId,
    type = Opplysningstype.LAND,
    verdi = verdi,
) {
    init {
        require(verdi.length == 3) {
            "ISO 3166-1-alpha3 må være 3 bokstaver lang. Fikk: $verdi"
        }
    }
}

class BooleanSvar(opplysningId: UUID, verdi: Boolean) : Svar<Boolean>(
    opplysningId = opplysningId,
    type = Opplysningstype.BOOLEAN,
    verdi = verdi,
)

class DatoSvar(opplysningId: UUID, verdi: LocalDate) : Svar<LocalDate>(
    opplysningId = opplysningId,
    type = Opplysningstype.DATO,
    verdi = verdi,
)

class TekstSvar(opplysningId: UUID, verdi: String) : Svar<String>(
    opplysningId = opplysningId,
    type = Opplysningstype.TEKST,
    verdi = verdi,
)

class PeriodesvarSvar(opplysningId: UUID, verdi: PeriodeSvar) : Svar<PeriodeSvar>(
    opplysningId = opplysningId,
    type = Opplysningstype.PERIODE,
    verdi = verdi,
)
