package no.nav.dagpenger.soknad.orkestrator.spørsmål.grupper

import no.nav.dagpenger.soknad.orkestrator.spørsmål.SpørsmålDTO

class SpørsmålgruppeDTO(
    val id: Int,
    val navn: Spørsmålgruppe,
    val nesteSpørsmål: SpørsmålDTO<*>?,
    val besvarteSpørsmål: List<SpørsmålDTO<*>>,
)
