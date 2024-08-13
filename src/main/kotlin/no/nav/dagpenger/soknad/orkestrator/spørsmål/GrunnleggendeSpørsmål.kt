package no.nav.dagpenger.soknad.orkestrator.spørsmål

import no.nav.dagpenger.soknad.orkestrator.api.models.SporsmalDTO
import no.nav.dagpenger.soknad.orkestrator.api.models.SporsmalTypeDTO
import java.util.UUID

data class GrunnleggendeSpørsmål(
    val id: Int,
    val tekstnøkkel: String,
    val type: SpørsmålType,
    val gyldigeSvar: List<String>,
)

fun GrunnleggendeSpørsmål.toSporsmalDTO(
    spørsmålId: UUID,
    svar: String?,
): SporsmalDTO =
    SporsmalDTO(
        id = spørsmålId,
        tekstnøkkel = tekstnøkkel,
        type = SporsmalTypeDTO.valueOf(type.name),
        svar = svar,
        gyldigeSvar = gyldigeSvar,
    )
