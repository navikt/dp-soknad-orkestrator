package no.nav.dagpenger.soknad.orkestrator.opplysning

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.soknad.orkestrator.db.Postgres.dataSource
import no.nav.dagpenger.soknad.orkestrator.db.Postgres.withMigratedDb
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
    fun `vi lagrer ikke opplysning dersom den er allerede lagret`() {
        val ident = "12345678901"
        val søknadsId = UUID.randomUUID()
        val behandlingsId = UUID.randomUUID()
        val opplysning1 = opplysningMed(ident = ident, søknadsId = søknadsId, behandlingsId = behandlingsId)
        val opplysning2 = opplysningMed(ident = ident, søknadsId = søknadsId, behandlingsId = behandlingsId)

        withMigratedDb {
            opplysningRepository.lagre(opplysning1)
            opplysningRepository.antall() shouldBe 1

            opplysningRepository.lagre(opplysning2)
            opplysningRepository.antall() shouldBe 1
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
    svar = listOf("svar"),
    ident = ident,
    søknadsId = søknadsId,
    behandlingsId = behandlingsId,
)
