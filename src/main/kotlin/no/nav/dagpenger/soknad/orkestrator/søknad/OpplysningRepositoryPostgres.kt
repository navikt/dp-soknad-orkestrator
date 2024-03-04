package no.nav.dagpenger.soknad.orkestrator.søknad

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID
import javax.sql.DataSource

class OpplysningRepositoryPostgres(dataSource: DataSource) : OpplysningRepository {
    val database = Database.connect(dataSource)

    init {
        transaction {
            // TODO Skal bruke flyway til å opprette tabellene
            SchemaUtils.createMissingTablesAndColumns(OpplysningTabell)
        }
    }

    override fun lagre(opplysning: Opplysning) {
        transaction {
            OpplysningTabell.insert {
                it[beskrivendeId] = opplysning.beskrivendeId()
                it[svar] = opplysning.svar().joinToString()
                it[fødselsnummer] = opplysning.fødselsnummer
                it[søknadsId] = opplysning.søknadsId
                it[behandlingsId] = opplysning.behandlingsId
            }
        }
    }

    override fun hent(
        beskrivendeId: String,
        fødselsnummer: String,
        søknadsId: UUID,
    ): Opplysning {
        return transaction {
            OpplysningTabell.selectAll()
                .where {
                    OpplysningTabell.beskrivendeId eq beskrivendeId and
                        (OpplysningTabell.fødselsnummer eq fødselsnummer) and
                        (OpplysningTabell.søknadsId eq søknadsId)
                }
                .map {
                    Opplysning(
                        beskrivendeId = it[OpplysningTabell.beskrivendeId],
                        svar = it[OpplysningTabell.svar].split(","),
                        fødselsnummer = it[OpplysningTabell.fødselsnummer],
                        søknadsId = it[OpplysningTabell.søknadsId],
                        behandlingsId = it[OpplysningTabell.behandlingsId],
                    )
                }.first()
        }
    }
}

object OpplysningTabell : Table() {
    val beskrivendeId = varchar("beskrivendeId", 255)
    val svar = varchar("svar", 255)
    val fødselsnummer = varchar("fødselsnummer", 11)
    val søknadsId = uuid("søknadsId").nullable()
    val behandlingsId = uuid("behandlingsId").nullable()
}
