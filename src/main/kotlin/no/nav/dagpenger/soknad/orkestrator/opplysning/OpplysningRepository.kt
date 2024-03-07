package no.nav.dagpenger.soknad.orkestrator.opplysning

import java.util.UUID

interface OpplysningRepository {
    fun lagre(opplysning: Opplysning)

    fun hent(
        beskrivendeId: String,
        ident: String,
        søknadsId: UUID,
        behandlingsId: UUID,
    ): Opplysning

    fun antall(): Long
}
