package no.nav.dagpenger.soknad.orkestrator.opplysning.db

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysning
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysningstype
import no.nav.dagpenger.soknad.orkestrator.opplysning.Svar
import no.nav.dagpenger.soknad.orkestrator.opplysning.seksjoner.Seksjon
import no.nav.dagpenger.soknad.orkestrator.opplysning.seksjoner.Seksjonsnavn
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadTabell
import no.nav.dagpenger.soknad.orkestrator.søknad.db.getId
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.json.jsonb
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource

class OpplysningRepository(
    dataSource: DataSource,
) {
    val database = Database.connect(dataSource)

    fun opprettSeksjon(
        søknadId: UUID,
        seksjon: Seksjon,
    ) {
        transaction {
            SeksjonTabell.insert {
                it[this.navn] = seksjon.navn.name
                it[this.versjon] = seksjon.versjon
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
                SeksjonTabell.getId(søknadDBId, opplysning.seksjonsnavn)
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
                        seksjonsnavn = Seksjonsnavn.valueOf(it[SeksjonTabell.navn]),
                        opplysningsbehovId = it[OpplysningTabell.opplysningsbehovId],
                        type = Opplysningstype.valueOf(it[OpplysningTabell.type]),
                        svar = it[OpplysningTabell.svar],
                    )
                }.firstOrNull()
        }

    fun lagreSvar(svar: Svar<*>) {
        transaction {
            val updatedRows =
                OpplysningTabell.update(where = { OpplysningTabell.opplysningId eq svar.opplysningId }) {
                    it[OpplysningTabell.svar] = svar
                    it[sistEndretAvBruker] = LocalDateTime.now()
                }
            if (updatedRows == 0) {
                throw IllegalStateException("Fant ikke opplysning med id ${svar.opplysningId}, kan ikke lagre svar")
            }
        }
    }

    fun hentAlle(søknadId: UUID): List<Opplysning> =
        transaction {
            val søknadDBId = SøknadTabell.getId(søknadId) ?: error("Fant ikke søknad med id $søknadId")

            (OpplysningTabell innerJoin SeksjonTabell)
                .selectAll()
                .where { OpplysningTabell.seksjonId eq SeksjonTabell.id }
                .andWhere { SeksjonTabell.søknadId eq søknadDBId }
                .map {
                    Opplysning(
                        opplysningId = it[OpplysningTabell.opplysningId],
                        seksjonsnavn = Seksjonsnavn.valueOf(it[SeksjonTabell.navn]),
                        opplysningsbehovId = it[OpplysningTabell.opplysningsbehovId],
                        type = Opplysningstype.valueOf(it[OpplysningTabell.type]),
                        svar = it[OpplysningTabell.svar],
                    )
                }
        }

    fun hentAlleForSeksjon(
        søknadId: UUID,
        seksjonsnavn: Seksjonsnavn,
    ): List<Opplysning> =
        transaction {
            val søknadDBId = SøknadTabell.getId(søknadId) ?: error("Fant ikke søknad med id $søknadId, kan ikke hente opplysninger")

            val seksjonDBId =
                SeksjonTabell.getId(søknadDBId, seksjonsnavn)
                    ?: error("Fant ikke seksjon med navn $seksjonsnavn for søknad med id $søknadId, kan ikke hente opplysninger")

            OpplysningTabell
                .selectAll()
                .where { OpplysningTabell.seksjonId eq seksjonDBId }
                .map {
                    Opplysning(
                        opplysningId = it[OpplysningTabell.opplysningId],
                        seksjonsnavn = seksjonsnavn,
                        opplysningsbehovId = it[OpplysningTabell.opplysningsbehovId],
                        type = Opplysningstype.valueOf(it[OpplysningTabell.type]),
                        svar = it[OpplysningTabell.svar],
                    )
                }
        }

    fun slett(
        søknadId: UUID,
        seksjonsnavn: Seksjonsnavn,
        opplysningsbehovId: Int,
    ) {
        transaction {
            val søknadDBId = SøknadTabell.getId(søknadId) ?: error("Fant ikke søknad med id $søknadId, kan ikke slette opplysning")
            val seksjonDBId =
                SeksjonTabell.getId(søknadDBId, seksjonsnavn)
                    ?: error("Fant ikke seksjon med navn $seksjonsnavn for søknad med id $søknadId, kan ikke slette opplysning")

            OpplysningTabell.deleteWhere {
                seksjonId eq seksjonDBId and
                    (OpplysningTabell.opplysningsbehovId eq opplysningsbehovId)
            }
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
    val navn: Column<String> = text("navn")
    val versjon: Column<String> = text("versjon")
    val erFullført: Column<Boolean> = bool("er_fullfort").default(false)
    val søknadId: Column<Int> = integer("soknad_id").references(SøknadTabell.id)
}

fun SeksjonTabell.getId(
    søknadDBId: Int,
    seksjonsnavn: Seksjonsnavn,
): Int? =
    SeksjonTabell
        .selectAll()
        .where { (søknadId eq søknadDBId) and (navn eq seksjonsnavn.name) }
        .singleOrNull()
        ?.get(SeksjonTabell.id)
        ?.value
