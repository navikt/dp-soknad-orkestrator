package no.nav.dagpenger.soknad.orkestrator.søknad.db

import com.fasterxml.jackson.databind.JsonNode
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.Søknad
import no.nav.dagpenger.soknad.orkestrator.søknad.Tilstand
import no.nav.dagpenger.soknad.orkestrator.søknad.Tilstand.INNSENDT
import no.nav.dagpenger.soknad.orkestrator.søknad.Tilstand.JOURNALFØRT
import no.nav.dagpenger.soknad.orkestrator.søknad.Tilstand.PÅBEGYNT
import no.nav.dagpenger.soknad.orkestrator.søknad.Tilstand.SLETTET_AV_SYSTEM
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadTabell.journalpostId
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadTabell.tilstand
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.dateTimeLiteral
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.json.jsonb
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.stringLiteral
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.upsert
import java.time.LocalDateTime
import java.time.LocalDateTime.now
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
                onUpdate = { it[tilstand] = stringLiteral(søknad.tilstand.name) },
            ) {
                it[søknadId] = søknad.søknadId
                it[ident] = søknad.ident
                it[tilstand] = søknad.tilstand.name
            }

            quizOpplysningRepository.lagreBarnSøknadMapping(søknadId = søknad.søknadId)

            søknad.opplysninger.forEach { quizOpplysningRepository.lagre(it) }
        }
    }

    fun opprett(søknad: Søknad): UUID =
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
                .map { mapToSøknad(it) }
                .firstOrNull()
        }

    fun hentPåbegynt(ident: String): Søknad? =
        transaction {
            SøknadTabell
                .selectAll()
                .where { SøknadTabell.ident eq ident }
                .andWhere { tilstand eq PÅBEGYNT.name }
                .map {
                    Søknad(
                        søknadId = it[SøknadTabell.søknadId],
                        ident = it[SøknadTabell.ident],
                        tilstand = Tilstand.valueOf(it[tilstand]),
                    )
                }.firstOrNull()
        }

    fun lagreKomplettSøknadData(
        søknadId: UUID,
        komplettSøknadData: JsonNode,
    ) {
        transaction {
            SøknadDataTabell.insert {
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

    fun slett(
        søknadId: UUID,
        ident: String,
    ): Int =
        transaction {
            val antallSlettedeRader =
                SøknadTabell.deleteWhere {
                    (SøknadTabell.søknadId eq søknadId) and (SøknadTabell.ident eq ident)
                }
            quizOpplysningRepository.slett(søknadId)

            antallSlettedeRader
        }

    fun verifiserAtSøknadEksistererOgTilhørerIdent(
        søknadId: UUID,
        ident: String,
    ) {
        transaction {
            val identSomEierSøknad =
                SøknadTabell
                    .select(SøknadTabell.ident)
                    .where { SøknadTabell.søknadId eq søknadId }
                    .map { it[SøknadTabell.ident] }
                    .firstOrNull()
            requireNotNull(identSomEierSøknad) { "Fant ikke søknad med ID $søknadId" }
            require(identSomEierSøknad == ident) { "Søknad $søknadId tilhører ikke identen som gjør kallet" }
        }
    }

    fun verifiserAtSøknadHarForventetTilstand(
        søknadId: UUID,
        forventetTilstand: Tilstand,
    ) {
        transaction {
            val tilstand =
                SøknadTabell
                    .select(SøknadTabell.tilstand)
                    .where { SøknadTabell.søknadId eq søknadId }
                    .map { it[SøknadTabell.tilstand] }
                    .firstOrNull()
            requireNotNull(tilstand) { "Fant ikke søknad med ID $søknadId" }
            check(forventetTilstand.name == tilstand) {
                "Søknad $søknadId har en annen tilstand ($tilstand) enn forventet (${forventetTilstand.name})"
            }
        }
    }

    fun slettSøknadSomSystem(
        søknadId: UUID,
        ident: String,
        slettetTidspunkt: LocalDateTime = now(),
    ) = transaction {
        verifiserAtSøknadEksistererOgTilhørerIdent(søknadId, ident)
        SøknadTabell.update({ SøknadTabell.søknadId eq søknadId and (SøknadTabell.ident eq ident) }) {
            it[SøknadTabell.slettetTidspunkt] = slettetTidspunkt
            it[SøknadTabell.tilstand] = SLETTET_AV_SYSTEM.name
        }
    }

    fun markerSøknadSomInnsendt(
        søknadId: UUID,
        ident: String,
        innsendtTidspunkt: LocalDateTime,
    ) {
        transaction {
            verifiserAtSøknadEksistererOgTilhørerIdent(søknadId, ident)
            SøknadTabell.update({ SøknadTabell.søknadId eq søknadId }) {
                it[tilstand] = INNSENDT.name
                it[SøknadTabell.innsendtTidspunkt] = innsendtTidspunkt
            }
        }
    }

    fun markerSøknadSomJournalført(
        søknadId: UUID,
        ident: String,
        journalpostId: String,
        journalførtTidspunkt: LocalDateTime,
    ) {
        transaction {
            verifiserAtSøknadEksistererOgTilhørerIdent(søknadId, ident)
            SøknadTabell.update({ SøknadTabell.søknadId eq søknadId }) {
                it[tilstand] = JOURNALFØRT.name
                it[SøknadTabell.journalpostId] = journalpostId
                it[SøknadTabell.journalførtTidspunkt] = journalførtTidspunkt
            }
        }
    }

    fun markerSøknadSomOppdatert(
        søknadId: UUID,
        ident: String,
        oppdatertTidspunkt: LocalDateTime = now(),
    ) = transaction {
        verifiserAtSøknadEksistererOgTilhørerIdent(søknadId, ident)
        SøknadTabell.update({ SøknadTabell.søknadId eq søknadId and (SøknadTabell.ident eq ident) }) {
            it[SøknadTabell.oppdatertTidspunkt] = dateTimeLiteral(oppdatertTidspunkt)
        }
    }

    fun hentAlleSøknaderSomErPåbegyntOgIkkeOppdatertPå7Dager(): List<Søknad> =
        transaction {
            SøknadTabell
                .selectAll()
                .where {
                    tilstand eq PÅBEGYNT.name and (
                        SøknadTabell.oppdatertTidspunkt.isNotNull() and (
                            SøknadTabell.oppdatertTidspunkt lessEq
                                now().minusDays(
                                    7,
                                )
                        )
                            or (
                                SøknadTabell.oppdatertTidspunkt.isNull() and (
                                    SøknadTabell.opprettet lessEq
                                        now().minusDays(
                                            7,
                                        )
                                )
                            )
                    )
                }.map { mapToSøknad(it) }
                .toList()
        }

    private fun mapToSøknad(resultRow: ResultRow) =
        Søknad(
            søknadId = resultRow[SøknadTabell.søknadId],
            ident = resultRow[SøknadTabell.ident],
            tilstand = Tilstand.valueOf(resultRow[tilstand]),
            opplysninger = quizOpplysningRepository.hentAlle(resultRow[SøknadTabell.søknadId]),
            oppdatertTidspunkt = resultRow[SøknadTabell.oppdatertTidspunkt],
            innsendtTidspunkt = resultRow[SøknadTabell.innsendtTidspunkt],
            journalpostId = resultRow[journalpostId],
            journalførtTidspunkt = resultRow[SøknadTabell.journalførtTidspunkt],
            slettetTidspunkt = resultRow[SøknadTabell.slettetTidspunkt],
        )

    fun hentSøknadIdFraJournalPostId(
        journalpostId: String,
        ident: String,
    ): UUID =
        transaction {
            SøknadTabell
                .select(SøknadTabell.søknadId)
                .where { SøknadTabell.journalpostId eq journalpostId }
                .andWhere { SøknadTabell.ident eq ident }
                .map { it[SøknadTabell.søknadId] }
                .firstOrNull()
                ?: throw IllegalStateException("Fant ikke søknad med journalpostId: $journalpostId for ident: $ident")
        }
}

object SøknadTabell : IntIdTable("soknad") {
    val opprettet: Column<LocalDateTime> = datetime("opprettet").default(now())
    val søknadId: Column<UUID> = uuid("soknad_id")
    val ident: Column<String> = varchar("ident", 11)
    val tilstand: Column<String> = text("tilstand").default(PÅBEGYNT.name)
    val oppdatertTidspunkt: Column<LocalDateTime> = datetime("oppdatert_tidspunkt")
    val innsendtTidspunkt: Column<LocalDateTime> = datetime("innsendt_tidspunkt")
    val journalpostId: Column<String> = varchar("journalpost_id", 32)
    val journalførtTidspunkt: Column<LocalDateTime> = datetime("journalfort_tidspunkt")
    val slettetTidspunkt: Column<LocalDateTime> = datetime("slettet_tidspunkt")
}

object SøknadDataTabell : IntIdTable("soknad_data") {
    val opprettet: Column<LocalDateTime> = datetime("opprettet").default(now())
    val søknadId: Column<UUID> = uuid("soknad_id").references(SøknadTabell.søknadId)
    val soknadData: Column<JsonNode> =
        jsonb("soknad_data", { serializeSøknadData(it) }, { deserializeSøknadData(it) })
}

private fun serializeSøknadData(søknadData: JsonNode): String = objectMapper.writeValueAsString(søknadData)

private fun deserializeSøknadData(søknadData: String): JsonNode = objectMapper.readTree(søknadData)
