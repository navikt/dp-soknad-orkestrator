package no.nav.dagpenger.soknad.orkestrator.søknad.seksjon

import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import org.jetbrains.exposed.sql.Database
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

            SeksjonTabell.upsert(
                SeksjonTabell.søknadId,
                SeksjonTabell.seksjonId,
                onUpdate = listOf(Pair(SeksjonTabell.json, stringLiteral(json))),
            ) {
                it[SeksjonTabell.søknadId] = søknadId
                it[SeksjonTabell.seksjonId] = seksjonId
                it[SeksjonTabell.json] = json
            }
        }
    }
}
