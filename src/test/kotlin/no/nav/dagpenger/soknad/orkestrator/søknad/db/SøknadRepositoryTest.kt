package no.nav.dagpenger.soknad.orkestrator.søknad.db

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.dagpenger.soknad.orkestrator.db.Postgres.dataSource
import no.nav.dagpenger.soknad.orkestrator.db.Postgres.withMigratedDb
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysning
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.Boolsk
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.Tekst
import no.nav.dagpenger.soknad.orkestrator.opplysning.db.OpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.opplysning.db.OpplysningRepositoryPostgres
import no.nav.dagpenger.soknad.orkestrator.søknad.Søknad
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test

class SøknadRepositoryTest {
    private lateinit var søknadRepository: SøknadRepository
    private lateinit var opplysningRepository: OpplysningRepository
    private val ident = "1234567890"

    @BeforeTest
    fun setup() {
        withMigratedDb {
            opplysningRepository = OpplysningRepositoryPostgres(dataSource)
            søknadRepository =
                SøknadRepository(
                    dataSource,
                    opplysningRepository,
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
    fun `kan slette søknad`() {
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
        søknadRepository.hent(søknadId) shouldNotBe null

        søknadRepository.slett(søknadId)
        søknadRepository.hent(søknadId) shouldBe null
    }

    @Test
    fun `sletting av søknad sletter også tilhørende opplysninger`() {
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
                        Opplysning(
                            beskrivendeId = "beskrivendeId2",
                            type = Boolsk,
                            svar = true,
                            ident = ident,
                            søknadId = søknadId,
                        ),
                    ),
            )

        søknadRepository.lagre(søknad)
        opplysningRepository.hentAlle(søknadId).size shouldBe 2

        søknadRepository.slett(søknadId)
        opplysningRepository.hentAlle(søknadId).size shouldBe 0
    }

    @Test
    fun `vi returnerer null dersom det ikke finnes en søknad med gitt id`() {
        withMigratedDb {
            søknadRepository.hent(UUID.randomUUID()) shouldBe null
        }
    }
}
