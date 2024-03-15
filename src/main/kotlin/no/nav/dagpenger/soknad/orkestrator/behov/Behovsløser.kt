package no.nav.dagpenger.soknad.orkestrator.behov

import no.nav.helse.rapids_rivers.RapidsConnection
import java.util.UUID

abstract class Behovsløser(val rapidsConnection: RapidsConnection) {
    abstract val behov: String

    abstract fun løs(
        ident: String,
        søknadsId: UUID,
        behandlingsId: UUID,
    )
}
