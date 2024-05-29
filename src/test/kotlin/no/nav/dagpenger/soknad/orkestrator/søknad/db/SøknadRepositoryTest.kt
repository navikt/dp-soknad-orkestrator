package no.nav.dagpenger.soknad.orkestrator.søknad.db

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.soknad.orkestrator.db.Postgres.dataSource
import no.nav.dagpenger.soknad.orkestrator.db.Postgres.withMigratedDb
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysning
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.Tekst
import no.nav.dagpenger.soknad.orkestrator.opplysning.db.OpplysningRepositoryPostgres
import no.nav.dagpenger.soknad.orkestrator.søknad.Søknad
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test

class SøknadRepositoryTest {
    private lateinit var søknadRepository: SøknadRepository
    private val ident = "1234567890"

    @BeforeTest
    fun setup() {
        withMigratedDb {
            søknadRepository =
                SøknadRepository(
                    dataSource,
                    OpplysningRepositoryPostgres(dataSource),
                )
        }
    }

    @Test
    fun `kan lagre og hente søknad`() {
        val søknadId = UUID.randomUUID()
        val søknad =
            Søknad(
                søknadId = søknadId,
                ident = ident,
                opplysninger =
                    listOf(
                        Opplysning(
                            beskrivendeId = "beskrivendeId",
                            type = Tekst,
                            svar = "Svar",
                            ident = ident,
                            søknadId = søknadId,
                        ),
                    ),
            )

        søknadRepository.lagre(søknad)
        val hentetSøknad = søknadRepository.hent(søknadId)

        hentetSøknad?.ident shouldBe søknad.ident
        hentetSøknad?.søknadId shouldBe søknad.søknadId
        hentetSøknad?.opplysninger?.size shouldBe 1
    }

    @Test
    fun `vi returnerer null dersom det ikke finnes en søknad med gitt id`() {
        withMigratedDb {
            søknadRepository.hent(UUID.randomUUID()) shouldBe null
        }
    }
}
