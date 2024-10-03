package no.nav.dagpenger.soknad.orkestrator.opplysning

import no.nav.dagpenger.soknad.orkestrator.api.models.OpplysningDTO
import no.nav.dagpenger.soknad.orkestrator.api.models.OpplysningstypeDTO
import java.util.UUID

data class Opplysningsbehov(
    val id: Int,
    val tekstnøkkel: String,
    val type: Opplysningstype,
    val gyldigeSvar: List<String>,
)

fun Opplysningsbehov.toOpplysningDTO(
    opplysningId: UUID,
    svar: String?,
): OpplysningDTO =
    OpplysningDTO(
        opplysningId = opplysningId,
        tekstnøkkel = tekstnøkkel,
        type = OpplysningstypeDTO.valueOf(type.name.lowercase()),
        svar = svar,
        gyldigeSvar = gyldigeSvar,
    )
