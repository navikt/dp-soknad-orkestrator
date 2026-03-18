package no.nav.dagpenger.soknad.orkestrator.opplysning

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.BarnSvar
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import javax.sql.DataSource

class SaksbehandlerBarnRepositoryPostgres(
    dataSource: DataSource,
) : SaksbehandlerBarnRepository {
    val database = Database.connect(dataSource)

    override fun hentBarn(søknadId: UUID): List<BarnSvar>? =
        transaction {
            SaksbehandlerBarnTabell
                .selectAll()
                .where { SaksbehandlerBarnTabell.søknadId eq søknadId }
                .orderBy(SaksbehandlerBarnTabell.opprettet to SortOrder.DESC, SaksbehandlerBarnTabell.id to SortOrder.DESC)
                .limit(1)
                .firstOrNull()
                ?.let { row ->
                    objectMapper.readValue<List<BarnSvar>>(row[SaksbehandlerBarnTabell.barn])
                }
        }

    override fun lagreBarn(
        søknadId: UUID,
        barn: List<BarnSvar>,
        endretAv: String,
    ) {
        transaction {
            SaksbehandlerBarnTabell.insert {
                it[SaksbehandlerBarnTabell.søknadId] = søknadId
                it[SaksbehandlerBarnTabell.barn] = objectMapper.writeValueAsString(barn)
                it[SaksbehandlerBarnTabell.endretAv] = endretAv
                it[SaksbehandlerBarnTabell.opprettet] = OffsetDateTime.now(ZoneOffset.UTC)
            }
        }
    }
}
