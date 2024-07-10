package no.nav.dagpenger.soknad.orkestrator.søknad.db

import java.util.UUID

class InMemorySøknadRepository {
    private val tabell = HashMap<UUID, List<LagretInfo>>()

    fun lagre(
        spørsmålId: UUID,
        søknadId: UUID,
        gruppeId: Int,
        idIGruppe: Int,
        svar: String?,
    ) {
        val dbRad =
            LagretInfo(
                spørsmålId = spørsmålId,
                gruppeId = gruppeId,
                idIGruppe = idIGruppe,
                svar = svar,
            )
        tabell[søknadId] = tabell[søknadId]
            ?.filter {
                it.gruppeId != gruppeId || it.idIGruppe != idIGruppe
            }?.plus(dbRad) ?: listOf(dbRad)
    }

    fun slett(
        søknadId: UUID,
        gruppeId: Int,
        spørsmålIdIGruppe: Int,
    ) {
        tabell[søknadId] =
            tabell[søknadId]
                ?.filter {
                    it.gruppeId != gruppeId || it.idIGruppe != spørsmålIdIGruppe
                }.orEmpty()
    }

    fun hent(
        søknadId: UUID,
        gruppeId: Int,
        spørsmålIdIGruppe: Int,
    ): LagretInfo? = tabell[søknadId]?.find { it.gruppeId == gruppeId && it.idIGruppe == spørsmålIdIGruppe }

    fun hent(
        søknadId: UUID,
        spørsmålId: UUID,
    ): LagretInfo? = tabell[søknadId]?.find { it.spørsmålId == spørsmålId }

    fun hentAlle(søknadId: UUID): List<LagretInfo> = tabell[søknadId] ?: emptyList()

    fun hentAlleKeys(): Set<UUID> = tabell.keys
}

data class LagretInfo(
    val spørsmålId: UUID,
    val gruppeId: Int,
    val idIGruppe: Int,
    val svar: String?,
)
