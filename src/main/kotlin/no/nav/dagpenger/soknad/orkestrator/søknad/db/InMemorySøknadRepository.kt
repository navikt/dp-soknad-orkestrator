package no.nav.dagpenger.soknad.orkestrator.søknad.db

import no.nav.dagpenger.soknad.orkestrator.api.models.SporsmalDTO
import java.util.UUID

class InMemorySøknadRepository {
    private val spørsmålgreier = HashMap<UUID, List<SporsmalDTO>>()

    fun lagre(
        søknadId: UUID,
        spørsmål: SporsmalDTO,
    ) {
        spørsmålgreier[søknadId] = spørsmålgreier[søknadId]?.filter { it.id != spørsmål.id }?.plus(spørsmål) ?: listOf(spørsmål)
    }

    fun hent(
        søknadId: UUID,
        tekstnøkkel: String,
    ): SporsmalDTO? {
        return spørsmålgreier[søknadId]?.find { it.tekstnøkkel == tekstnøkkel }
    }

    fun hentAlle(søknadId: UUID): List<SporsmalDTO> {
        return spørsmålgreier[søknadId] ?: emptyList()
    }

    fun hentAlleKeys(): Set<UUID> {
        return spørsmålgreier.keys
    }
}
