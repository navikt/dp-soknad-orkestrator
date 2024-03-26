package no.nav.dagpenger.soknad.orkestrator.utils

import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysning
import no.nav.dagpenger.soknad.orkestrator.opplysning.db.OpplysningRepository
import java.util.UUID

class InMemoryOpplysningRepository : OpplysningRepository {
    private val opplysninger = mutableListOf<Opplysning<*>>()

    override fun lagre(opplysning: Opplysning<*>) {
        opplysninger.add(opplysning)
    }

    override fun hent(
        beskrivendeId: String,
        ident: String,
        søknadsId: UUID,
    ): Opplysning<*> {
        return opplysninger.find {
            it.beskrivendeId == beskrivendeId && it.ident == ident && it.søknadsId == søknadsId
        }
            ?: throw IllegalArgumentException("Fant ikke opplysning")
    }
}
