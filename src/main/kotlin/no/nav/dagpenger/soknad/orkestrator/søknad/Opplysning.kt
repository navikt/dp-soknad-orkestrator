package no.nav.dagpenger.soknad.orkestrator.søknad

import java.util.UUID

class Opplysning(
    private val beskrivendeId: String,
    private val svar: List<String>,
    val fødselsnummer: String,
    val søknadsId: UUID? = null,
    val behandlingsId: UUID? = null,
) {
    fun svar() = svar

    fun beskrivendeId() = beskrivendeId
}
