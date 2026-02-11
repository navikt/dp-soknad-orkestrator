package no.nav.dagpenger.soknad.orkestrator.søknad

import java.time.LocalDateTime
import java.util.UUID

data class SøknadForIdent(
    val søknadId: UUID,
    val innsendtTimestamp: LocalDateTime,
    val status: String,
    var tittel: String = "Søknad om dagpenger",
)
