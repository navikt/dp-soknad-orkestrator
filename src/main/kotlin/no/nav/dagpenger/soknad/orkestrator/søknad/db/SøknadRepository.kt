package no.nav.dagpenger.soknad.orkestrator.søknad.db

import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.Søknad
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
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
            SøknadTabell.insertIgnore {
                it[søknadId] = søknad.søknadId
                it[ident] = søknad.ident
            }

            søknad.opplysninger.forEach { quizOpplysningRepository.lagre(it) }
        }
    }

    fun lagre(søknad: Søknad) {
        transaction {
            SøknadTabell.insertIgnore {
                it[søknadId] = søknad.søknadId
                it[ident] = søknad.ident
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
                        opplysninger = quizOpplysningRepository.hentAlle(søknadId),
                    )
                }.firstOrNull()
        }

    fun slett(søknadId: UUID) {
        transaction {
            SøknadTabell.deleteWhere { SøknadTabell.søknadId eq søknadId }
            quizOpplysningRepository.slett(søknadId)
        }
    }
}

object SøknadTabell : IntIdTable("soknad") {
    val opprettet: Column<LocalDateTime> = datetime("opprettet").default(LocalDateTime.now())
    val søknadId: Column<UUID> = uuid("soknad_id")
    val ident: Column<String> = varchar("ident", 11)
}

fun SøknadTabell.getId(søknadId: UUID) =
    SøknadTabell
        .select(SøknadTabell.id)
        .where { SøknadTabell.søknadId eq søknadId }
        .singleOrNull()
        ?.get(SøknadTabell.id)
        ?.value
