package no.nav.dagpenger.soknad.orkestrator.opplysning.db

import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysning
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysningstype
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadTabell
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource

class OpplysningRepository(
    dataSource: DataSource,
) {
    val database = Database.connect(dataSource)

    fun lagre(
        søknadId: UUID,
        opplysning: Opplysning,
    ) {
        transaction {
            val søknadDBId =
                SøknadTabell.select(SøknadTabell.id)
                    .where { SøknadTabell.søknadId eq søknadId }
                    .singleOrNull()?.get(SøknadTabell.id)?.value
                    ?: error("Fant ikke søknad med id $søknadId")

            val seksjonDBId =
                SeksjonTabell.select(SeksjonTabell.id)
                    .where { SeksjonTabell.søknadId eq søknadDBId }
                    .singleOrNull()?.get(SøknadTabell.id)?.value
                    ?: error("Fant ikke seksjon med id $søknadId")

            OpplysningTabell.insert {
                it[opplysningId] = opplysning.opplysningId
                it[seksjonId] = seksjonDBId
                it[opplysningsbehovId] = opplysning.opplysningsbehovId
                it[type] = opplysning.type.name
                // it[svar] = jacksonObjectMapper().writeValueAsString(opplysning.svar)
            }
        }
    }

    fun hent(opplysningId: UUID): Opplysning? {
        return transaction {
            OpplysningTabell
                .select(OpplysningTabell.opplysningId eq opplysningId)
                .map {
                    Opplysning(
                        opplysningId = it[OpplysningTabell.opplysningId],
                        opplysningsbehovId = it[OpplysningTabell.opplysningsbehovId],
                        type = Opplysningstype.valueOf(it[OpplysningTabell.type]),
                        svar = null,
                        // svar = jacksonObjectMapper().readValue(it[OpplysningTabell.svar], Svar::class.java),
                    )
                }
                .firstOrNull()
        }
    }

    fun slett(opplysningId: UUID) {
        transaction {
            OpplysningTabell.deleteWhere { OpplysningTabell.opplysningId eq opplysningId }
        }
    }
}

object OpplysningTabell : IntIdTable("opplysning") {
    val opprettet: Column<LocalDateTime> = datetime("opprettet").default(LocalDateTime.now())
    val opplysningId: Column<UUID> = uuid("opplysning_id")
    val sistEndretAvBruker: Column<LocalDateTime?> = datetime("sist_endret_av_bruker").nullable()
    val seksjonId: Column<Int> = integer("seksjon_id").references(SeksjonTabell.id)
    val opplysningsbehovId: Column<Int> = integer("opplysningsbehov_id")
    val type: Column<String> = text("type")
    val svar: Column<String?> = text("svar").nullable()
}

object SeksjonTabell : IntIdTable("seksjon") {
    val versjon: Column<String> = text("versjon")
    val erFullført: Column<Boolean> = bool("er_fullfort").default(false)
    val søknadId: Column<Int> = integer("soknad_id").references(SøknadTabell.id)
}
