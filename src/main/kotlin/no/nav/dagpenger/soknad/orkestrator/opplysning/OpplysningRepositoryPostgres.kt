package no.nav.dagpenger.soknad.orkestrator.opplysning

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.ResultRow
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
            OpplysningTabell.leggTil(opplysning)
        }
    }

    override fun hent(
        beskrivendeId: String,
        fødselsnummer: String,
        søknadsId: UUID,
    ): Opplysning {
        return transaction {
            OpplysningTabell
                .selectAll()
                .somMatcher(beskrivendeId, fødselsnummer, søknadsId)
                .map(tilOpplysning())
                .firstOrNull()
                ?: throw NoSuchElementException(
                    "Ingen opplysning funnet med gitt beskrivendeId:" + " $beskrivendeId," +
                        " fødselsnummer: $fødselsnummer, " +
                        "og søknadsId: $søknadsId",
                )
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

fun OpplysningTabell.leggTil(opplysning: Opplysning) {
    insert {
        it[beskrivendeId] = opplysning.beskrivendeId()
        it[svar] = opplysning.svar().joinToString()
        it[fødselsnummer] = opplysning.fødselsnummer
        it[søknadsId] = opplysning.søknadsId
        it[behandlingsId] = opplysning.behandlingsId
    }
}

fun Query.somMatcher(
    beskrivendeId: String,
    fødselsnummer: String,
    søknadsId: UUID,
): Query =
    where {
        OpplysningTabell.beskrivendeId eq beskrivendeId and
            (OpplysningTabell.fødselsnummer eq fødselsnummer) and
            (OpplysningTabell.søknadsId eq søknadsId)
    }

private fun tilOpplysning(): (ResultRow) -> Opplysning =
    {
        Opplysning(
            beskrivendeId = it[OpplysningTabell.beskrivendeId],
            svar = it[OpplysningTabell.svar].split(","),
            fødselsnummer = it[OpplysningTabell.fødselsnummer],
            søknadsId = it[OpplysningTabell.søknadsId],
            behandlingsId = it[OpplysningTabell.behandlingsId],
        )
    }
