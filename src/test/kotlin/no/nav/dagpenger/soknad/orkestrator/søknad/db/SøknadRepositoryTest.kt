package no.nav.dagpenger.soknad.orkestrator.søknad.db

import BarnSøknadMappingTabell
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import no.nav.dagpenger.soknad.orkestrator.db.Postgres.dataSource
import no.nav.dagpenger.soknad.orkestrator.db.Postgres.withMigratedDb
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.QuizOpplysning
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Boolsk
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Tekst
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepositoryPostgres
import no.nav.dagpenger.soknad.orkestrator.søknad.Søknad
import no.nav.dagpenger.soknad.orkestrator.søknad.Tilstand
import no.nav.dagpenger.soknad.orkestrator.søknad.Tilstand.INNSENDT
import no.nav.dagpenger.soknad.orkestrator.søknad.Tilstand.JOURNALFØRT
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime.now
import java.util.UUID
import java.util.UUID.randomUUID
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
        val søknadId = randomUUID()
        val søknad =
            Søknad(
                søknadId = søknadId,
                ident = ident,
                tilstand = INNSENDT,
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
        val søknadbarnId = hentSøknadbarnIdUtenÅOppretteNy(søknadId)

        hentetSøknad?.ident shouldBe søknad.ident
        hentetSøknad?.søknadId shouldBe søknad.søknadId
        hentetSøknad?.tilstand shouldBe søknad.tilstand
        hentetSøknad?.opplysninger?.size shouldBe 1
        søknadbarnId shouldNotBe null
    }

    @Test
    fun `oppdaterer bare tilstand når vi lagrer en søknad som allerede er lagret`() {
        val søknadId = randomUUID()
        val søknad = Søknad(søknadId, ident)
        søknadRepository.lagre(søknad)
        val sammeSøknadMedNyTilstand = Søknad(søknadId, "ident2", tilstand = INNSENDT)
        søknadRepository.lagreQuizSøknad(sammeSøknadMedNyTilstand)

        val hentetSøknad = søknadRepository.hent(søknadId)

        hentetSøknad?.ident shouldBe søknad.ident
        hentetSøknad?.søknadId shouldBe søknad.søknadId
        hentetSøknad?.tilstand shouldBe INNSENDT
    }

    @Test
    fun `hentPåbegynt henter påbegynt søknad for en gitt ident`() {
        val søknadId = randomUUID()
        val søknad = Søknad(søknadId = søknadId, ident = ident, tilstand = Tilstand.PÅBEGYNT)

        søknadRepository.lagre(søknad)

        val hentetSøknad = søknadRepository.hentPåbegynt(ident)

        hentetSøknad?.ident shouldBe søknad.ident
        hentetSøknad?.søknadId shouldBe søknad.søknadId
        hentetSøknad?.tilstand shouldBe søknad.tilstand
    }

    @Test
    fun `hentPåbegynt returnerer null hvis det ikke finnes en søknad for gitt ident`() {
        val hentetSøknad = søknadRepository.hentPåbegynt(ident)

        hentetSøknad shouldBe null
    }

    @Test
    fun `Kan lagre og hente komplett søknaddata`() {
        val søknadId = randomUUID()
        søknadRepository.lagre(Søknad(søknadId = søknadId, ident = "1234567891"))

        søknadRepository.lagreKomplettSøknadData(søknadId, komplettSøknaddata)
        val hentetSøknaddata = søknadRepository.hentKomplettSøknadData(søknadId)

        hentetSøknaddata shouldBe komplettSøknaddata
    }

    @Test
    fun `Kan ikke lagre komplett søknaddata for én søknad flere ganger`() {
        val søknadId = randomUUID()
        søknadRepository.lagre(Søknad(søknadId = søknadId, ident = "1234567891"))

        søknadRepository.lagreKomplettSøknadData(søknadId, komplettSøknaddata)

        shouldThrow<ExposedSQLException> {
            søknadRepository.lagreKomplettSøknadData(søknadId, komplettSøknaddata)
        }
    }

    @Test
    fun `kan slette søknad`() {
        val søknadId = randomUUID()
        val søknad =
            Søknad(
                søknadId = søknadId,
                ident = ident,
                tilstand = INNSENDT,
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

        søknadRepository.slett(søknadId, ident)
        søknadRepository.hent(søknadId) shouldBe null
    }

    @Test
    fun `kan ikke slette søknad siden ident er ulik`() {
        val søknadId = randomUUID()
        val søknad =
            Søknad(
                søknadId = søknadId,
                ident = ident,
                tilstand = INNSENDT,
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

        søknadRepository.slett(søknadId, "en-annen-ident")
        søknadRepository.hent(søknadId) shouldNotBe null
    }

    @Test
    fun `sletting av søknad sletter også tilhørende opplysninger`() {
        val søknadId = randomUUID()
        val søknad =
            Søknad(
                søknadId = søknadId,
                ident = ident,
                tilstand = INNSENDT,
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

        søknadRepository.slett(søknadId, ident)
        opplysningRepository.hentAlle(søknadId).size shouldBe 0
    }

    @Test
    fun `vi returnerer null dersom det ikke finnes en søknad med gitt id`() {
        withMigratedDb {
            søknadRepository.hent(randomUUID()) shouldBe null
        }
    }

    @Test
    fun `markerSøknadSomInnsendt markerer søknaden som innsendt`() {
        val søknadId = randomUUID()
        val innsendtTidspunkt = now().withNano(0)
        søknadRepository.lagre(Søknad(søknadId, "ident"))

        søknadRepository.markerSøknadSomInnsendt(søknadId, innsendtTidspunkt)

        with(søknadRepository.hent(søknadId)) {
            this shouldNotBe null
            this?.tilstand shouldBe INNSENDT
            this?.innsendtTidspunkt shouldBe innsendtTidspunkt
        }
    }

    @Test
    fun `markerSøknadSomJournalført markerer søknaden som journalført`() {
        val søknadId = randomUUID()
        val journalpostId = "239874323"
        val journalførtTidspunkt = now().withNano(0)
        søknadRepository.lagre(Søknad(søknadId, "ident"))

        søknadRepository.markerSøknadSomJournalført(søknadId, journalpostId, journalførtTidspunkt)

        with(søknadRepository.hent(søknadId)) {
            this shouldNotBe null
            this?.tilstand shouldBe JOURNALFØRT
            this?.journalpostId shouldBe journalpostId
            this?.journalførtTidspunkt shouldBe journalførtTidspunkt
        }
    }
}

fun hentSøknadbarnIdUtenÅOppretteNy(søknadId: UUID): UUID? =
    transaction {
        BarnSøknadMappingTabell
            .select(BarnSøknadMappingTabell.id, BarnSøknadMappingTabell.søknadbarnId)
            .where { BarnSøknadMappingTabell.søknadId eq søknadId }
            .firstOrNull()
            ?.get(BarnSøknadMappingTabell.søknadbarnId)
    }

private val komplettSøknaddata =
    objectMapper.readTree(
        //language=JSON
        """
        {
          "ident": "12345678901",
          "søknadId": "3a8c70ed-a902-47aa-9ba2-ae5ea6448c4d",
          "seksjoner": {
            "seksjoner": [
              {
                "fakta": [
                  {
                    "id": "6001",
                    "svar": "NOR",
                    "type": "land",
                    "beskrivendeId": "faktum.hvilket-land-bor-du-i"
                  }
                ],
                "beskrivendeId": "bostedsland"
              }
            ]
          }
        }
        """.trimIndent(),
    )
