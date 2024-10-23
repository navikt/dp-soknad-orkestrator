package no.nav.dagpenger.soknad.orkestrator.søknad.db

import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.Søknad
import no.nav.dagpenger.soknad.orkestrator.søknad.Tilstand
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.javatime.datetime
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

    fun lagre(søknad: Søknad) {
        transaction {
            SøknadTabell.insertIgnore {
                it[søknadId] = søknad.søknadId
                it[ident] = søknad.ident
                it[tilstand] = søknad.tilstand.name
            }
        }
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

    fun slett(søknadId: UUID): Int {
        return transaction {
            val antallSlettedeRader = SøknadTabell.deleteWhere { SøknadTabell.søknadId eq søknadId }
            quizOpplysningRepository.slett(søknadId)

            antallSlettedeRader
        }
    }
}

object SøknadTabell : IntIdTable("soknad") {
    val opprettet: Column<LocalDateTime> = datetime("opprettet").default(LocalDateTime.now())
    val søknadId: Column<UUID> = uuid("soknad_id")
    val ident: Column<String> = varchar("ident", 11)
    val tilstand: Column<String> = text("tilstand").default(Tilstand.PÅBEGYNT.name)
}

fun SøknadTabell.getId(søknadId: UUID) =
    SøknadTabell
        .select(SøknadTabell.id)
        .where { SøknadTabell.søknadId eq søknadId }
        .singleOrNull()
        ?.get(SøknadTabell.id)
        ?.value
