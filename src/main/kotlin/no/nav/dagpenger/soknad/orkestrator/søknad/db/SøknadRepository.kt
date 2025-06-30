package no.nav.dagpenger.soknad.orkestrator.søknad.db

import com.fasterxml.jackson.databind.JsonNode
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.Søknad
import no.nav.dagpenger.soknad.orkestrator.søknad.Tilstand
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
import org.jetbrains.exposed.sql.stringLiteral
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.upsert
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource

class SøknadRepository(
    dataSource: DataSource,
    private val quizOpplysningRepository: QuizOpplysningRepository,
) {
    val database = Database.connect(dataSource)

    fun lagreQuizSøknad(søknad: Søknad) {
        transaction {
            // Hvis søknadId allerede finnes oppdaterer vi bare tilstand for søknaden
            SøknadTabell.upsert(
                SøknadTabell.søknadId,
                onUpdate = listOf(Pair(SøknadTabell.tilstand, stringLiteral(søknad.tilstand.name))),
            ) {
                it[søknadId] = søknad.søknadId
                it[ident] = søknad.ident
                it[tilstand] = søknad.tilstand.name
            }

            søknad.opplysninger.forEach { quizOpplysningRepository.lagre(it) }
        }
    }

    fun lagre(søknad: Søknad): UUID =
        transaction {
            SøknadTabell.insert {
                it[søknadId] = søknad.søknadId
                it[ident] = søknad.ident
                it[tilstand] = søknad.tilstand.name
            }[SøknadTabell.søknadId]
        }

    fun hent(søknadId: UUID): Søknad? =
        transaction {
            SøknadTabell
                .selectAll()
                .where { SøknadTabell.søknadId eq søknadId }
                .map {
                    Søknad(
                        søknadId = it[SøknadTabell.søknadId],
                        ident = it[SøknadTabell.ident],
                        tilstand = Tilstand.valueOf(it[SøknadTabell.tilstand]),
                        opplysninger = quizOpplysningRepository.hentAlle(søknadId),
                    )
                }.firstOrNull()
        }

    fun hentPåbegynt(ident: String): Søknad? =
        transaction {
            SøknadTabell
                .selectAll()
                .where { SøknadTabell.ident eq ident }
                .andWhere { SøknadTabell.tilstand eq Tilstand.PÅBEGYNT.name }
                .map {
                    Søknad(
                        søknadId = it[SøknadTabell.søknadId],
                        ident = it[SøknadTabell.ident],
                        tilstand = Tilstand.valueOf(it[SøknadTabell.tilstand]),
                    )
                }.firstOrNull()
        }

    fun lagreKomplettSøknadData(
        søknadId: UUID,
        komplettSøknadData: JsonNode,
    ) {
        transaction {
            SøknadDataTabell.insert { it ->
                it[SøknadDataTabell.søknadId] = søknadId
                it[soknadData] = komplettSøknadData
            }
        }
    }

    fun hentKomplettSøknadData(søknadId: UUID): JsonNode? =
        transaction {
            SøknadDataTabell
                .select(SøknadDataTabell.soknadData)
                .where { SøknadDataTabell.søknadId eq søknadId }
                .singleOrNull()
                ?.get(SøknadDataTabell.soknadData)
        }

    fun slett(søknadId: UUID): Int =
        transaction {
            val antallSlettedeRader = SøknadTabell.deleteWhere { SøknadTabell.søknadId eq søknadId }
            quizOpplysningRepository.slett(søknadId)

            antallSlettedeRader
        }
}

object SøknadTabell : IntIdTable("soknad") {
    val opprettet: Column<LocalDateTime> = datetime("opprettet").default(LocalDateTime.now())
    val søknadId: Column<UUID> = uuid("soknad_id")
    val ident: Column<String> = varchar("ident", 11)
    val tilstand: Column<String> = text("tilstand").default(Tilstand.PÅBEGYNT.name)
}

object SøknadDataTabell : IntIdTable("soknad_data") {
    val opprettet: Column<LocalDateTime> = datetime("opprettet").default(LocalDateTime.now())
    val søknadId: Column<UUID> = uuid("soknad_id").references(SøknadTabell.søknadId)
    val soknadData: Column<JsonNode> =
        jsonb("soknad_data", { serializeSøknadData(it) }, { deserializeSøknadData(it) })
}

private fun serializeSøknadData(søknadData: JsonNode): String = objectMapper.writeValueAsString(søknadData)

private fun deserializeSøknadData(søknadData: String): JsonNode = objectMapper.readTree(søknadData)

fun SøknadTabell.getId(søknadId: UUID) =
    SøknadTabell
        .select(SøknadTabell.id)
        .where { SøknadTabell.søknadId eq søknadId }
        .singleOrNull()
        ?.get(SøknadTabell.id)
        ?.value
