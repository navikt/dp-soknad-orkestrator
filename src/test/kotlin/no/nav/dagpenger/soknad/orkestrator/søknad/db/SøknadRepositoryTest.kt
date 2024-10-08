package no.nav.dagpenger.soknad.orkestrator.søknad.db

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.dagpenger.soknad.orkestrator.db.Postgres.dataSource
import no.nav.dagpenger.soknad.orkestrator.db.Postgres.withMigratedDb
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.QuizOpplysning
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Boolsk
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Tekst
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepositoryPostgres
import no.nav.dagpenger.soknad.orkestrator.søknad.Søknad
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test

class SøknadRepositoryTest {
    private lateinit var søknadRepository: SøknadRepository
    private lateinit var opplysningRepository: QuizOpplysningRepository
    private val ident = "1234567890"

    @BeforeTest
    fun setup() {
        withMigratedDb {
            opplysningRepository = QuizOpplysningRepositoryPostgres(dataSource)
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
                        QuizOpplysning(
                            beskrivendeId = "beskrivendeId",
                            type = Tekst,
                            svar = "Svar",
                            ident = ident,
                            søknadId = søknadId,
                        ),
                    ),
            )

        søknadRepository.lagreQuizSøknad(søknad)
        val hentetSøknad = søknadRepository.hent(søknadId)

        hentetSøknad?.ident shouldBe søknad.ident
        hentetSøknad?.søknadId shouldBe søknad.søknadId
        hentetSøknad?.tilstand shouldBe søknad.tilstand
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
                        QuizOpplysning(
                            beskrivendeId = "beskrivendeId",
                            type = Tekst,
                            svar = "Svar",
                            ident = ident,
                            søknadId = søknadId,
                        ),
                    ),
            )

        søknadRepository.lagreQuizSøknad(søknad)
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
                        QuizOpplysning(
                            beskrivendeId = "beskrivendeId",
                            type = Tekst,
                            svar = "Svar",
                            ident = ident,
                            søknadId = søknadId,
                        ),
                        QuizOpplysning(
                            beskrivendeId = "beskrivendeId2",
                            type = Boolsk,
                            svar = true,
                            ident = ident,
                            søknadId = søknadId,
                        ),
                    ),
            )

        søknadRepository.lagreQuizSøknad(søknad)
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
