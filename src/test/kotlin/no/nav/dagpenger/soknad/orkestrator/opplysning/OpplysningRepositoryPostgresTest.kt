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
            Opplysning(
                beskrivendeId = beskrivendeId,
                svar = listOf("svar1"),
                ident = ident,
                søknadsId = søknadsId,
                behandlingsId = behandlingsId,
            )

        withMigratedDb {
            opplysningRepository.lagre(opplysning)

            val hentetOpplysning =
                opplysningRepository.hent(
                    beskrivendeId,
                    ident,
                    søknadsId,
                    behandlingsId,
                )

            hentetOpplysning.beskrivendeId shouldBe beskrivendeId
            hentetOpplysning.svar shouldBe listOf("svar1")
            hentetOpplysning.ident shouldBe ident
        }
    }
}
