package no.nav.dagpenger.soknad.orkestrator.opplysning.db

import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysning
import java.util.UUID

interface OpplysningRepository {
    fun lagre(opplysning: Opplysning<*>)

    fun hent(
        beskrivendeId: String,
        ident: String,
        søknadId: UUID,
    ): Opplysning<*>?

    fun hentAlle(søknadId: UUID): List<Opplysning<*>>

    fun slett(søknadId: UUID)
}
