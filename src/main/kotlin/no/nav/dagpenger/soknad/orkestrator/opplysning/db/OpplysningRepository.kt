package no.nav.dagpenger.soknad.orkestrator.opplysning.db

import com.fasterxml.jackson.module.kotlin.readValue
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysning
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysningstype
import no.nav.dagpenger.soknad.orkestrator.opplysning.Svar
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadTabell
import no.nav.dagpenger.soknad.orkestrator.søknad.db.getId
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.json.jsonb
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class OpplysningRepository(
    dataSource: DataSource,
) {
    val database = Database.connect(dataSource)

    fun opprettSeksjon(
        søknadId: UUID,
        versjon: String,
    ) {
        transaction {
            SeksjonTabell.insert {
                it[this.versjon] = versjon
                it[this.søknadId] = SøknadTabell.getId(søknadId)
                    ?: error("Fant ikke søknad med id $søknadId")
            }
        }
    }

    fun lagre(
        søknadId: UUID,
        opplysning: Opplysning,
    ) {
        transaction {
            val søknadDBId =
                SøknadTabell.getId(søknadId)
                    ?: error("Fant ikke søknad med id $søknadId")

            val seksjonDBId =
                SeksjonTabell
                    .select(SeksjonTabell.id)
                    .where { SeksjonTabell.søknadId eq søknadDBId }
                    .andWhere { SeksjonTabell.versjon eq opplysning.seksjonversjon }
                    .singleOrNull()
                    ?.get(SeksjonTabell.id)
                    ?.value
                    ?: error("Fant ikke seksjon med søknadId $søknadId")

            OpplysningTabell.insert {
                it[opplysningId] = opplysning.opplysningId
                it[seksjonId] = seksjonDBId
                it[opplysningsbehovId] = opplysning.opplysningsbehovId
                it[type] = opplysning.type.name
                it[svar] = opplysning.svar
            }
        }
    }

    fun hent(opplysningId: UUID): Opplysning? =
        transaction {
            (OpplysningTabell innerJoin SeksjonTabell)
                .selectAll()
                .where { OpplysningTabell.seksjonId eq SeksjonTabell.id }
                .andWhere { OpplysningTabell.opplysningId eq opplysningId }
                .map {
                    Opplysning(
                        opplysningId = it[OpplysningTabell.opplysningId],
                        seksjonversjon = it[SeksjonTabell.versjon],
                        opplysningsbehovId = it[OpplysningTabell.opplysningsbehovId],
                        type = Opplysningstype.valueOf(it[OpplysningTabell.type]),
                        svar = it[OpplysningTabell.svar],
                    )
                }.firstOrNull()
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
    val svar: Column<Svar<*>?> = jsonb("svar", { serializeSvar(it) }, { deserializeSvar(it) }).nullable()
}

fun serializeSvar(svar: Svar<*>): String = objectMapper.writeValueAsString(svar)
fun deserializeSvar(svar: String): Svar<*> = objectMapper.readValue(svar)

object SeksjonTabell : IntIdTable("seksjon") {
    val versjon: Column<String> = text("versjon")
    val erFullført: Column<Boolean> = bool("er_fullfort").default(false)
    val søknadId: Column<Int> = integer("soknad_id").references(SøknadTabell.id)
}
