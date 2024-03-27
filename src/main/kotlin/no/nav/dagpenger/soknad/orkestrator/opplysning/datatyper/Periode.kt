package no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper

import com.fasterxml.jackson.databind.JsonNode
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysning
import no.nav.helse.rapids_rivers.asLocalDate
import java.time.LocalDate
import java.util.UUID

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
