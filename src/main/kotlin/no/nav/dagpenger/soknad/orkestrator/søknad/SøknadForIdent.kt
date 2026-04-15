package no.nav.dagpenger.soknad.orkestrator.søknad

import java.time.LocalDateTime
import java.util.UUID

data class SøknadForIdent(
    val søknadId: UUID,
    val innsendtTimestamp: LocalDateTime? = null,
    val oppdatertTidspunkt: LocalDateTime? = null,
    val status: String,
    var tittel: String = "Søknad om dagpenger",
)

data class SøknadForIdentMedOrkestratorKildeSjekk(
    val søknadId: UUID,
    val innsendtTimestamp: LocalDateTime? = null,
    val oppdatertTidspunkt: LocalDateTime? = null,
    val status: String,
    val tittel: String = "Søknad om dagpenger",
    val erOrkestratorSøknad: Boolean,
) {
    constructor(base: SøknadForIdent, erOrkestratorSøknad: Boolean) : this(
        søknadId = base.søknadId,
        innsendtTimestamp = base.innsendtTimestamp,
        oppdatertTidspunkt = base.oppdatertTidspunkt,
        status = base.status,
        tittel = base.tittel,
        erOrkestratorSøknad = erOrkestratorSøknad,
    )
}
