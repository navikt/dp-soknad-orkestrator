package no.nav.dagpenger.soknad.orkestrator.opplysning

import java.util.UUID

interface OpplysningRepository {
    fun lagre(opplysning: Opplysning)

    fun hent(
        beskrivendeId: String,
        fødselsnummer: String,
        søknadsId: UUID,
    ): Opplysning
}
