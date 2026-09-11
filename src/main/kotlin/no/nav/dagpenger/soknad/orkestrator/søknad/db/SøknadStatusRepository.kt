package no.nav.dagpenger.soknad.orkestrator.søknad.db

import no.nav.dagpenger.soknad.orkestrator.søknad.Status
import no.nav.dagpenger.soknad.orkestrator.søknad.SøknadStatus
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.json.json
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID
import javax.sql.DataSource

class SøknadStatusRepository(
    dataSource: DataSource,
) {
    val database = Database.connect(dataSource)

    fun lagre(søknadStatus: SøknadStatus) {
        transaction {
            SøknadStatusTabell.insert {
                it[søknadId] = søknadStatus.søknadId
                it[ident] = søknadStatus.ident
                it[behandlingId] = søknadStatus.behandlingId
                it[førteTil] = søknadStatus.førteTil.name
                it[rettighetsperioder] = søknadStatus.rettighetsperioder
            }
        }
    }

    fun hent(søknadId: UUID): SøknadStatus? =
        transaction {
            SøknadStatusTabell
                .selectAll()
                .where { SøknadStatusTabell.søknadId eq søknadId }
                .orderBy(SøknadStatusTabell.id, SortOrder.DESC)
                .limit(1)
                .map { mapToSøknadStatus(it) }
                .firstOrNull()
        }

    private fun mapToSøknadStatus(resultRow: ResultRow) =
        SøknadStatus(
            søknadId = resultRow[SøknadStatusTabell.søknadId],
            ident = resultRow[SøknadStatusTabell.ident],
            behandlingId = resultRow[SøknadStatusTabell.behandlingId],
            førteTil = Status.valueOf(resultRow[SøknadStatusTabell.førteTil]),
            rettighetsperioder = resultRow[SøknadStatusTabell.rettighetsperioder],
        )
}

object SøknadStatusTabell : LongIdTable("soknad_status") {
    val søknadId: Column<UUID> = uuid("soknad_id").references(SøknadTabell.søknadId)
    val ident: Column<String> = varchar("ident", 11)
    val behandlingId: Column<String> = text("behandling_id")
    val førteTil: Column<String> = text("forte_til")
    val rettighetsperioder: Column<String> = json<String>("rettighetsperioder", { it }, { it })
}
