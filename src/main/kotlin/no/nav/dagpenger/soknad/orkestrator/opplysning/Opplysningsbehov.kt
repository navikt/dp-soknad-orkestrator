package no.nav.dagpenger.soknad.orkestrator.opplysning

import no.nav.dagpenger.soknad.orkestrator.api.models.SporsmalDTO
import no.nav.dagpenger.soknad.orkestrator.api.models.SporsmalTypeDTO
import java.util.UUID

data class Opplysningsbehov(
    val id: Int,
    val tekstnøkkel: String,
    val type: Opplysningstype,
    val gyldigeSvar: List<String>,
)

fun Opplysningsbehov.toSporsmalDTO(
    spørsmålId: UUID,
    svar: String?,
): SporsmalDTO =
    SporsmalDTO(
        id = spørsmålId,
        tekstnøkkel = tekstnøkkel,
        type = SporsmalTypeDTO.valueOf(type.name.lowercase()),
        svar = svar,
        gyldigeSvar = gyldigeSvar,
    )
