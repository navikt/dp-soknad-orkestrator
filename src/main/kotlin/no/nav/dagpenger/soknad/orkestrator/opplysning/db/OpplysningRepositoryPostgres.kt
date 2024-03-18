package no.nav.dagpenger.soknad.orkestrator.opplysning.db

import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysning
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID
import javax.sql.DataSource

class OpplysningRepositoryPostgres(dataSource: DataSource) : OpplysningRepository {
    val database = Database.connect(dataSource)

    override fun lagre(opplysning: Opplysning) {
        transaction {
            if (!opplysningEksisterer(opplysning)) {
                OpplysningTabell.leggTil(opplysning)
            }
        }
    }

    override fun hent(
        beskrivendeId: String,
        ident: String,
        søknadsId: UUID,
    ): Opplysning {
        return transaction {
            OpplysningTabell
                .selectAll()
                .somMatcher(beskrivendeId, ident, søknadsId)
                .map(tilOpplysning())
                .firstOrNull()
                ?: throw NoSuchElementException(
                    "Ingen opplysning funnet med gitt beskrivendeId:" + " $beskrivendeId," +
                        " ident: $ident, " +
                        "og søknadsId: $søknadsId",
                )
        }
    }
}

object OpplysningTabell : Table("opplysning") {
    val beskrivendeId = varchar("beskrivende_id", 255)
    val svar = varchar("svar", 255)
    val ident = varchar("ident", 11)
    val søknadsId = uuid("soknads_id").nullable()
}

private fun opplysningEksisterer(opplysning: Opplysning): Boolean =
    OpplysningTabell.selectAll().somMatcher(
        opplysning.beskrivendeId,
        opplysning.ident,
        opplysning.søknadsId,
    ).any()

fun OpplysningTabell.leggTil(opplysning: Opplysning) {
    insert {
        it[beskrivendeId] = opplysning.beskrivendeId
        it[svar] = opplysning.svar
        it[ident] = opplysning.ident
        it[søknadsId] = opplysning.søknadsId
    }
}

fun Query.somMatcher(
    beskrivendeId: String,
    ident: String,
    søknadsId: UUID?,
): Query =
    where {
        OpplysningTabell.beskrivendeId eq beskrivendeId and
            (OpplysningTabell.ident eq ident) and
            (OpplysningTabell.søknadsId eq søknadsId)
    }

private fun tilOpplysning(): (ResultRow) -> Opplysning =
    {
        Opplysning(
            beskrivendeId = it[OpplysningTabell.beskrivendeId],
            svar = it[OpplysningTabell.svar],
            ident = it[OpplysningTabell.ident],
            søknadsId = it[OpplysningTabell.søknadsId],
        )
    }
