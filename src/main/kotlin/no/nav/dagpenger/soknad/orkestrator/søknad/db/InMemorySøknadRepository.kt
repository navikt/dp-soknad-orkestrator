package no.nav.dagpenger.soknad.orkestrator.søknad.db

import no.nav.dagpenger.soknad.orkestrator.spørsmål.SpørsmålType
import no.nav.dagpenger.soknad.orkestrator.spørsmål.grupper.Spørsmålgruppenavn
import java.util.UUID

class InMemorySøknadRepository {
    private val tabell = HashMap<UUID, List<Spørsmål>>()

    fun lagre(
        søknadId: UUID,
        spørsmål: Spørsmål,
    ) {
        val dbRad = spørsmål
        tabell[søknadId] = tabell[søknadId]
            ?.filter {
                it.gruppenavn != spørsmål.gruppenavn || it.gruppespørsmålId != spørsmål.gruppespørsmålId
            }?.plus(dbRad) ?: listOf(dbRad)
    }

    fun slett(
        søknadId: UUID,
        gruppenavn: Spørsmålgruppenavn,
        gruppespørsmålId: Int,
    ) {
        tabell[søknadId] =
            tabell[søknadId]
                ?.filter {
                    it.gruppenavn != gruppenavn || it.gruppespørsmålId != gruppespørsmålId
                }.orEmpty()
    }

    fun hent(
        søknadId: UUID,
        gruppenavn: Spørsmålgruppenavn,
        gruppespørsmålId: Int,
    ): Spørsmål? = tabell[søknadId]?.find { it.gruppenavn == gruppenavn && it.gruppespørsmålId == gruppespørsmålId }

    fun hent(
        søknadId: UUID,
        spørsmålId: UUID,
    ): Spørsmål? = tabell[søknadId]?.find { it.spørsmålId == spørsmålId }

    fun hentAlle(søknadId: UUID): List<Spørsmål> = tabell[søknadId] ?: emptyList()

    fun hentAlleKeys(): Set<UUID> = tabell.keys
}

data class Spørsmål(
    val spørsmålId: UUID,
    val gruppenavn: Spørsmålgruppenavn,
    val gruppespørsmålId: Int,
    val type: SpørsmålType,
    val svar: String?,
)
