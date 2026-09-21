package no.nav.dagpenger.soknad.orkestrator.søknad

import java.util.UUID

data class SøknadStatus(
    val søknadId: UUID,
    val ident: String,
    val behandlingId: String,
    val førteTil: Status,
    val rettighetsperioder: String,
)

data class Rettighetsperiode(
    val fraOgMed: String,
    val tilOgMed: String,
    val harRett: Boolean,
    val opprinnelse: String,
)

enum class Status {
    Innvilgelse,
    Avslag,
    Stans,
    Gjenopptak,
    Endring,
    Ukjent,
}
