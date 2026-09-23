package no.nav.dagpenger.soknad.orkestrator.søknad

import java.util.UUID

data class SøknadStatus(
    val søknadId: UUID,
    val ident: String,
    val behandlingId: String,
    val førteTil: Status,
    val rettighetsperioder: String,
)

enum class Status {
    Innvilgelse,
    Avslag,
    Stans,
    Gjenopptak,
    Endring,
    Ukjent,
}
