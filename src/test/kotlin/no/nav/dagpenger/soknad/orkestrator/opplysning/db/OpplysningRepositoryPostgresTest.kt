package no.nav.dagpenger.soknad.orkestrator.opplysning.db

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.soknad.orkestrator.db.Postgres.dataSource
import no.nav.dagpenger.soknad.orkestrator.db.Postgres.withMigratedDb
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysning
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import java.util.UUID

class OpplysningRepositoryPostgresTest {
    private var opplysningRepository = OpplysningRepositoryPostgres(dataSource)

    @Test
    fun `vi kan lagre opplysning`() {
        val beskrivendeId = "beskrivendeId"
        val ident = "12345678901"
        val søknadsId = UUID.randomUUID()
        val behandlingsId = UUID.randomUUID()
        val opplysning =
            opplysningMed(
                beskrivendeId = beskrivendeId,
                ident = ident,
                søknadsId = søknadsId,
                behandlingsId = behandlingsId,
            )

        withMigratedDb {
            opplysningRepository.lagre(opplysning)

            opplysningRepository.hent(
                beskrivendeId,
                ident,
                søknadsId,
                behandlingsId,
            ) shouldBe opplysning
        }
    }

    @Test
    fun `vi lagrer ikke opplysning dersom den allerede er lagret`() {
        val ident = "12345678901"
        val søknadsId = UUID.randomUUID()
        val behandlingsId = UUID.randomUUID()
        val opplysning1 = opplysningMed(ident = ident, søknadsId = søknadsId, behandlingsId = behandlingsId)
        val opplysning2 = opplysningMed(ident = ident, søknadsId = søknadsId, behandlingsId = behandlingsId)

        withMigratedDb {
            opplysningRepository.lagre(opplysning1)
            val antallEtterFørsteLagring = transaction { OpplysningTabell.selectAll().count() }
            antallEtterFørsteLagring shouldBe 1

            opplysningRepository.lagre(opplysning2)
            val antallEtterAndreLagring = transaction { OpplysningTabell.selectAll().count() }
            antallEtterAndreLagring shouldBe 1
        }
    }
}

fun opplysningMed(
    ident: String,
    beskrivendeId: String = "beskrivendeId",
    søknadsId: UUID = UUID.randomUUID(),
    behandlingsId: UUID = UUID.randomUUID(),
) = Opplysning(
    beskrivendeId = beskrivendeId,
    svar = "svar",
    ident = ident,
    søknadsId = søknadsId,
    behandlingsId = behandlingsId,
)
