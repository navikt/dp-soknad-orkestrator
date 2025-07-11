package no.nav.dagpenger.soknad.orkestrator.søknad.seksjon

import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.stringLiteral
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.upsert
import java.util.UUID
import javax.sql.DataSource

class SeksjonRepository(
    val dataSource: DataSource,
    val søknadRepository: SøknadRepository,
) {
    val database = Database.connect(dataSource)

    fun lagre(
        søknadId: UUID,
        seksjonId: String,
        json: String,
    ) {
        transaction {
            requireNotNull(søknadRepository.hent(søknadId)) { "Fant ikke søknad med ID {søknadId}." }

            SeksjonV2Tabell.upsert(
                SeksjonV2Tabell.søknadId,
                SeksjonV2Tabell.seksjonId,
                onUpdate = listOf(Pair(SeksjonV2Tabell.json, stringLiteral(json))),
            ) {
                it[SeksjonV2Tabell.søknadId] = søknadId
                it[SeksjonV2Tabell.seksjonId] = seksjonId
                it[SeksjonV2Tabell.json] = json
            }
        }
    }

    fun hent(
        søknadId: UUID,
        seksjonId: String,
    ): String? =
        transaction {
            SeksjonV2Tabell
                .select(SeksjonV2Tabell.json)
                .where {
                    SeksjonV2Tabell.søknadId eq søknadId and (SeksjonV2Tabell.seksjonId eq seksjonId)
                }.map {
                    it[SeksjonV2Tabell.json]
                }.firstOrNull()
        }

    fun hentSeksjoner(søknadId: UUID): List<Seksjon> =
        transaction {
            SeksjonV2Tabell
                .select(SeksjonV2Tabell.json)
                .where { SeksjonV2Tabell.søknadId eq søknadId }
                .map {
                    Seksjon(
                        seksjonId = it[SeksjonV2Tabell.seksjonId],
                        data = it[SeksjonV2Tabell.json],
                    )
                }.toList()
        }
}
