package no.nav.dagpenger.soknad.orkestrator.søknad.db

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import no.nav.dagpenger.soknad.orkestrator.db.Postgres.dataSource
import no.nav.dagpenger.soknad.orkestrator.db.Postgres.withMigratedDb
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.QuizOpplysning
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Barn
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.BarnSvar
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Boolsk
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Tekst
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepositoryPostgres
import no.nav.dagpenger.soknad.orkestrator.søknad.Søknad
import no.nav.dagpenger.soknad.orkestrator.søknad.Tilstand
import no.nav.dagpenger.soknad.orkestrator.søknad.Tilstand.INNSENDT
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
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
        val søknadbarnId =
            transaction {
                opplysningRepository.mapTilSøknadbarnId(søknadId)
            }

        hentetSøknad?.ident shouldBe søknad.ident
        hentetSøknad?.søknadId shouldBe søknad.søknadId
        hentetSøknad?.tilstand shouldBe søknad.tilstand
        hentetSøknad?.opplysninger?.size shouldBe 1
        søknadbarnId shouldBe null
    }

    // TODO: Gjør ferdig denne testen
    @Test
    fun `lagrer søknadbarnId når det finnes barn-opplysning i søknaden`() {
        val søknadId = UUID.randomUUID()
        val søknad =
            Søknad(
                søknadId = søknadId,
                ident = ident,
                tilstand = INNSENDT,
                opplysninger =
                    listOf(
                        QuizOpplysning(
                            beskrivendeId = "faktum.barn-liste",
                            type = Barn,
                            svar =
                                listOf(
                                    BarnSvar(
                                        barnSvarId = UUID.randomUUID(),
                                        fornavnOgMellomnavn = "Test",
                                        etternavn = "Testesen",
                                        fødselsdato = LocalDate.now(),
                                        statsborgerskap = "NOR",
                                        forsørgerBarnet = true,
                                        fraRegister = true,
                                        kvalifisererTilBarnetillegg = true,
                                    ),
                                ),
                            ident = ident,
                            søknadId = søknadId,
                        ),
                    ),
            )

        søknadRepository.lagreQuizSøknad(søknad)
        val hentetSøknad = søknadRepository.hent(søknadId)
        val søknadbarnId =
            transaction {
                opplysningRepository.mapTilSøknadbarnId(søknadId)
            }

        hentetSøknad?.ident shouldBe søknad.ident
        hentetSøknad?.søknadId shouldBe søknad.søknadId
        hentetSøknad?.tilstand shouldBe søknad.tilstand
        hentetSøknad?.opplysninger?.size shouldBe 1
        søknadbarnId shouldNotBe null
    }

    @Test
    fun `oppdaterer bare tilstand når vi lagrer en søknad som allerede er lagret`() {
        val søknadId = UUID.randomUUID()
        val søknad = Søknad(søknadId, "123456780")
        søknadRepository.lagre(søknad)
        val sammeSøknadMedNyTilstand = Søknad(søknadId, "123456780", tilstand = INNSENDT)

        søknadRepository.lagreQuizSøknad(sammeSøknadMedNyTilstand)

        søknadRepository.hent(søknadId)?.tilstand shouldBe INNSENDT
    }

    @Test
    fun `hentPåbegynt henter påbegynt søknad for en gitt ident`() {
        val søknadId = UUID.randomUUID()
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
        val søknadId = UUID.randomUUID()
        søknadRepository.lagre(Søknad(søknadId = søknadId, ident = "1234567891"))

        søknadRepository.lagreKomplettSøknadData(søknadId, komplettSøknaddata)
        val hentetSøknaddata = søknadRepository.hentKomplettSøknadData(søknadId)

        hentetSøknaddata shouldBe komplettSøknaddata
    }

    @Test
    fun `Kan ikke lagre komplett søknaddata for én søknad flere ganger`() {
        val søknadId = UUID.randomUUID()
        søknadRepository.lagre(Søknad(søknadId = søknadId, ident = "1234567891"))

        søknadRepository.lagreKomplettSøknadData(søknadId, komplettSøknaddata)

        shouldThrow<ExposedSQLException> {
            søknadRepository.lagreKomplettSøknadData(søknadId, komplettSøknaddata)
        }
    }

    @Test
    fun `kan slette søknad`() {
        val søknadId = UUID.randomUUID()
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
          },
          "orkestratorSeksjoner": [
            {
              "seksjonsnavn": "BOSTEDSLAND",
              "opplysninger": [
                {
                  "opplysningId": "a34beb9c-0fa6-48a8-a9b6-0b0dde283ae5",
                  "tekstnøkkel": "tekstnøkkel.periode",
                  "type": "PERIODE",
                  "svar": {
                    "fom": "2024-11-11",
                    "tom": "2024-11-11"
                  },
                  "gyldigeSvar": null
                }
              ]
            }
          ]
        }
        """.trimIndent(),
    )
