package no.nav.dagpenger.soknad.orkestrator.opplysning

import java.util.UUID

class Opplysning(
    val beskrivendeId: String,
    val svar: List<String>,
    val fødselsnummer: String,
    val søknadsId: UUID? = null,
    val behandlingsId: UUID? = null,
) {
    override fun equals(other: Any?) =
        other is Opplysning &&
            other.fødselsnummer == fødselsnummer &&
            other.søknadsId == søknadsId &&
            other.beskrivendeId == beskrivendeId &&
            other.svar == svar

    override fun hashCode() =
        "${this.fødselsnummer}${this.søknadsId}${this.beskrivendeId}"
            .hashCode()
}
