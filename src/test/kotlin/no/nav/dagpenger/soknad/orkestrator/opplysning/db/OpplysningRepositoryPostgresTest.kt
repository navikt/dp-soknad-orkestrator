package no.nav.dagpenger.soknad.orkestrator.opplysning.db

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.soknad.orkestrator.db.Postgres.dataSource
import no.nav.dagpenger.soknad.orkestrator.db.Postgres.withMigratedDb
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysning
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import java.util.NoSuchElementException
import java.util.UUID

class OpplysningRepositoryPostgresTest {
    private var opplysningRepository = OpplysningRepositoryPostgres(dataSource)

    @Test
    fun `vi kan lagre og hente opplysning`() {
        val beskrivendeId = "beskrivendeId"
        val ident = "12345678901"
        val søknadsId = UUID.randomUUID()
        val opplysning =
            opplysningMed(
                beskrivendeId = beskrivendeId,
                ident = ident,
                søknadsId = søknadsId,
            )

        withMigratedDb {
            opplysningRepository.lagre(opplysning)

            opplysningRepository.hent(
                beskrivendeId,
                ident,
                søknadsId,
            ) shouldBe opplysning
        }
    }

    @Test
    fun `vi lagrer ikke opplysning dersom den allerede er lagret`() {
        val ident = "12345678901"
        val søknadsId = UUID.randomUUID()
        val opplysning1 = opplysningMed(ident = ident, søknadsId = søknadsId)
        val opplysning2 = opplysningMed(ident = ident, søknadsId = søknadsId)

        withMigratedDb {
            opplysningRepository.lagre(opplysning1)
            val antallEtterFørsteLagring = transaction { OpplysningTabell.selectAll().count() }
            antallEtterFørsteLagring shouldBe 1

            opplysningRepository.lagre(opplysning2)
            val antallEtterAndreLagring = transaction { OpplysningTabell.selectAll().count() }
            antallEtterAndreLagring shouldBe 1
        }
    }

    @Test
    fun `vi henter ikke opplysning dersom ett av kriteriene ikke stemmer`() {
        val beskrivendeId = "dagpenger-søknadsdato"
        val ident = "12345678910"
        val søknadsId = UUID.randomUUID()

        val opplysning =
            Opplysning(
                beskrivendeId = beskrivendeId,
                svar = "2021-01-01",
                ident = ident,
                søknadsId = søknadsId,
            )

        withMigratedDb { opplysningRepository.lagre(opplysning) }

        shouldThrow<NoSuchElementException> {
            opplysningRepository.hent(
                beskrivendeId = beskrivendeId,
                ident = ident,
                søknadsId = UUID.randomUUID(),
            )
        }
    }
}

fun opplysningMed(
    ident: String,
    beskrivendeId: String = "beskrivendeId",
    søknadsId: UUID = UUID.randomUUID(),
) = Opplysning(
    beskrivendeId = beskrivendeId,
    svar = "svar",
    ident = ident,
    søknadsId = søknadsId,
)
