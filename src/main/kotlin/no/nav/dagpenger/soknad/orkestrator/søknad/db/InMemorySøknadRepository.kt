package no.nav.dagpenger.soknad.orkestrator.søknad.db

import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysningstype
import no.nav.dagpenger.soknad.orkestrator.opplysning.Svar
import no.nav.dagpenger.soknad.orkestrator.opplysning.grupper.Seksjonsnavn
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

    fun lagreSvar(
        søknadId: UUID,
        svar: Svar<*>,
    ) {
        val spørsmål = hent(søknadId, svar.opplysningId)
        val besvartSpørsmål = spørsmål.copy(svar = svar)

        lagre(søknadId, besvartSpørsmål)
    }

    fun slettSpørsmål(
        søknadId: UUID,
        gruppenavn: Seksjonsnavn,
        gruppespørsmålId: Int,
    ) {
        tabell[søknadId] =
            tabell[søknadId]
                ?.filter {
                    it.gruppenavn != gruppenavn || it.gruppespørsmålId != gruppespørsmålId
                }.orEmpty()
    }

    fun slettSøknad(søknadId: UUID) {
        tabell.remove(søknadId)
    }

    fun hent(
        søknadId: UUID,
        gruppenavn: Seksjonsnavn,
        gruppespørsmålId: Int,
    ): Spørsmål? = tabell[søknadId]?.find { it.gruppenavn == gruppenavn && it.gruppespørsmålId == gruppespørsmålId }

    fun hent(
        søknadId: UUID,
        spørsmålId: UUID,
    ): Spørsmål =
        tabell[søknadId]?.find { it.spørsmålId == spørsmålId }
            ?: throw IllegalArgumentException("Fant ikke spørsmål med id: $spørsmålId")

    fun hentGruppeinfo(
        søknadId: UUID,
        spørsmålId: UUID,
    ): Pair<Int?, Seksjonsnavn?> {
        val gruppespørsmålId = tabell[søknadId]?.find { it.spørsmålId == spørsmålId }?.gruppespørsmålId
        val gruppenavn = tabell[søknadId]?.find { it.spørsmålId == spørsmålId }?.gruppenavn

        return Pair(gruppespørsmålId, gruppenavn)
    }

    fun hentAlle(søknadId: UUID): List<Spørsmål> = tabell[søknadId] ?: emptyList()

    fun hentAlleKeys(): Set<UUID> = tabell.keys
}

data class Spørsmål(
    val spørsmålId: UUID,
    val gruppenavn: Seksjonsnavn,
    val gruppespørsmålId: Int,
    val type: Opplysningstype,
    val svar: Svar<*>?,
)
