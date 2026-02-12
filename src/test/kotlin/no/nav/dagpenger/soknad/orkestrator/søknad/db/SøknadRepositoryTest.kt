package no.nav.dagpenger.soknad.orkestrator.søknad.db

import BarnSøknadMappingTabell
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContainAnyOf
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
import no.nav.dagpenger.soknad.orkestrator.søknad.Tilstand.PÅBEGYNT
import no.nav.dagpenger.soknad.orkestrator.søknad.Tilstand.SLETTET_AV_SYSTEM
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonRepository
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.util.UUID
import java.util.UUID.randomUUID
import kotlin.test.BeforeTest
import kotlin.test.Test

class SøknadRepositoryTest {
    private lateinit var søknadRepository: SøknadRepository
    private lateinit var opplysningRepository: QuizOpplysningRepository
    private lateinit var seksjonRepository: SeksjonRepository
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
            seksjonRepository = SeksjonRepository(dataSource, søknadRepository)
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
        søknadRepository.opprett(søknad)
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
        val søknad = Søknad(søknadId = søknadId, ident = ident, tilstand = PÅBEGYNT)

        søknadRepository.opprett(søknad)

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
    fun `Kan opprette og hente komplett søknaddata`() {
        val søknadId = randomUUID()
        søknadRepository.opprett(Søknad(søknadId = søknadId, ident = "1234567891"))

        søknadRepository.lagreKomplettSøknadData(søknadId, komplettSøknaddata)
        val hentetSøknaddata = søknadRepository.hentKomplettSøknadData(søknadId)

        hentetSøknaddata shouldBe komplettSøknaddata
    }

    @Test
    fun `Kan ikke lagre komplett søknaddata for én søknad flere ganger`() {
        val søknadId = randomUUID()
        søknadRepository.opprett(Søknad(søknadId = søknadId, ident = "1234567891"))

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
    fun `hent returnerer null hvis søknaden ikke eksisterer`() {
        søknadRepository.hent(randomUUID()) shouldBe null
    }

    @Test
    fun `markerSøknadSomInnsendt markerer søknaden som innsendt`() {
        val søknadId = randomUUID()
        val innsendtTidspunkt = now().withNano(0)
        søknadRepository.opprett(Søknad(søknadId, ident))

        søknadRepository.markerSøknadSomInnsendt(søknadId, ident, innsendtTidspunkt)

        with(søknadRepository.hent(søknadId)) {
            this shouldNotBe null
            this?.tilstand shouldBe INNSENDT
            this?.innsendtTidspunkt shouldBe innsendtTidspunkt
        }
    }

    @Test
    fun `markerSøknadSomInnsendt kaster exception hvis søknaden ikke eksisterer`() {
        val søknadId = randomUUID()

        val exception =
            shouldThrow<IllegalArgumentException> {
                søknadRepository.markerSøknadSomInnsendt(søknadId, ident, now().withNano(0))
            }

        exception.message shouldBe "Fant ikke søknad med ID $søknadId"
    }

    @Test
    fun `markerSøknadSomInnsendt kaster exception hvis søknaden ikke tilhører bruker som gjør kallet`() {
        val søknadId = randomUUID()
        søknadRepository.opprett(Søknad(søknadId, ident))

        val exception =
            shouldThrow<IllegalArgumentException> {
                søknadRepository.markerSøknadSomInnsendt(søknadId, "en-annen-ident", now().withNano(0))
            }

        exception.message shouldBe "Søknad $søknadId tilhører ikke identen som gjør kallet"
    }

    @Test
    fun `markerSøknadSomJournalført markerer søknaden som journalført`() {
        val søknadId = randomUUID()
        val journalpostId = "239874323"
        val journalførtTidspunkt = now().withNano(0)
        søknadRepository.opprett(Søknad(søknadId, ident))

        søknadRepository.markerSøknadSomJournalført(søknadId, ident, journalpostId, journalførtTidspunkt)

        with(søknadRepository.hent(søknadId)) {
            this shouldNotBe null
            this?.tilstand shouldBe JOURNALFØRT
            this?.journalpostId shouldBe journalpostId
            this?.journalførtTidspunkt shouldBe journalførtTidspunkt
        }
    }

    @Test
    fun `markerSøknadSomJournalført kaster exception hvis søknaden ikke eksisterer`() {
        val søknadId = randomUUID()

        val exception =
            shouldThrow<IllegalArgumentException> {
                søknadRepository.markerSøknadSomJournalført(søknadId, ident, "239874323", now().withNano(0))
            }

        exception.message shouldBe "Fant ikke søknad med ID $søknadId"
    }

    @Test
    fun `markerSøknadSomJournalført kaster exception hvis søknaden ikke tilhører bruker som gjør kallet`() {
        val søknadId = randomUUID()
        søknadRepository.opprett(Søknad(søknadId, ident))

        val exception =
            shouldThrow<IllegalArgumentException> {
                søknadRepository.markerSøknadSomJournalført(søknadId, "en-annen-ident", "239874323", now().withNano(0))
            }

        exception.message shouldBe "Søknad $søknadId tilhører ikke identen som gjør kallet"
    }

    @Test
    fun `hentAlleSøknaderSomErPåbegyntOgIkkeOppdatertPå7Dager returnerer forentet resultat`() {
        val påbegyntSøknadSomBleOpprettetForMindreEnn7DagerSidenOgIkkeErOppdatert =
            opprettSøknad(randomUUID(), PÅBEGYNT, opprettetTidspunkt = now().minusDays(1))
        val påbegyntSøknadSomBleOpprettetFor7DagerSidenOgIkkeErOppdatert =
            opprettSøknad(randomUUID(), PÅBEGYNT, opprettetTidspunkt = now().minusDays(7).plusSeconds(5))
        val påbegyntSøknadSomBleOpprettetForMerEnn7DagerSidenOgIkkeErOppdatert =
            opprettSøknad(randomUUID(), PÅBEGYNT, opprettetTidspunkt = now().minusDays(8))
        val påbegyntSøknadSomErOppdatertForMindreEnn7DagerSiden =
            opprettSøknad(randomUUID(), PÅBEGYNT, oppdatertTidspunkt = now().minusDays(1))
        val påbegyntSøknadSomErOppdatertFor7DagerSiden =
            opprettSøknad(randomUUID(), PÅBEGYNT, oppdatertTidspunkt = now().minusDays(7).plusSeconds(5))
        val påbegyntSøknadSomErOppdatertForMerEnn7DagerSiden =
            opprettSøknad(randomUUID(), PÅBEGYNT, oppdatertTidspunkt = now().minusDays(8))
        val innsendtSøknadSomIkkeErOppdatert = opprettSøknad(randomUUID(), INNSENDT)
        val innsendtSøknadSomErOppdatertForMindreEnn7DagerSiden =
            opprettSøknad(randomUUID(), INNSENDT, oppdatertTidspunkt = now().minusDays(1))
        val innsendtSøknadSomErOppdatertFor7DagerSiden =
            opprettSøknad(randomUUID(), INNSENDT, oppdatertTidspunkt = now().minusDays(7).plusSeconds(5))
        val innsendtSøknadSomErOppdatertForMerEnn7DagerSiden =
            opprettSøknad(randomUUID(), INNSENDT, oppdatertTidspunkt = now().minusDays(8))
        val journalførtSøknadSomErOppdatertForMindreEnn7DagerSiden =
            opprettSøknad(randomUUID(), JOURNALFØRT, oppdatertTidspunkt = now().minusDays(1))
        val journalførtSøknadSomErOppdatertFor7DagerSiden =
            opprettSøknad(randomUUID(), JOURNALFØRT, oppdatertTidspunkt = now().minusDays(7).plusSeconds(5))
        val journalførtSøknadSomErOppdatertForMerEnn7DagerSiden =
            opprettSøknad(randomUUID(), JOURNALFØRT, oppdatertTidspunkt = now().minusDays(8))

        val søknader = søknadRepository.hentAlleSøknaderSomErPåbegyntOgIkkeOppdatertPå7Dager()

        søknader.size shouldBe 2
        with(søknader.map { søknad -> søknad.søknadId }) {
            this.shouldNotContainAnyOf(
                påbegyntSøknadSomBleOpprettetForMindreEnn7DagerSidenOgIkkeErOppdatert,
                påbegyntSøknadSomBleOpprettetFor7DagerSidenOgIkkeErOppdatert,
                påbegyntSøknadSomErOppdatertFor7DagerSiden,
                påbegyntSøknadSomErOppdatertForMindreEnn7DagerSiden,
                innsendtSøknadSomIkkeErOppdatert,
                innsendtSøknadSomErOppdatertForMindreEnn7DagerSiden,
                innsendtSøknadSomErOppdatertFor7DagerSiden,
                innsendtSøknadSomErOppdatertForMerEnn7DagerSiden,
                journalførtSøknadSomErOppdatertForMindreEnn7DagerSiden,
                journalførtSøknadSomErOppdatertFor7DagerSiden,
                journalførtSøknadSomErOppdatertForMerEnn7DagerSiden,
            )
            this.shouldContainAll(
                påbegyntSøknadSomBleOpprettetForMerEnn7DagerSidenOgIkkeErOppdatert,
                påbegyntSøknadSomErOppdatertForMerEnn7DagerSiden,
            )
        }
    }

    @Test
    fun `verifiserAtSøknadEksistererOgTilhørerIdent kaster exception hvis søknaden ikke eksisterer`() {
        val søknadId = randomUUID()
        val exception =
            shouldThrow<IllegalArgumentException> {
                søknadRepository.verifiserAtSøknadEksistererOgTilhørerIdent(søknadId, ident)
            }

        exception.message shouldBe "Fant ikke søknad med ID $søknadId"
    }

    @Test
    fun `verifiserAtSøknadEksistererOgTilhørerIdent kaster exception hvis søknaden ikke tilhører bruker som gjør kallet`() {
        val søknadId = randomUUID()
        søknadRepository.opprett(Søknad(søknadId, ident))

        val exception =
            shouldThrow<IllegalArgumentException> {
                søknadRepository.verifiserAtSøknadEksistererOgTilhørerIdent(søknadId, "en-annen-ident")
            }

        exception.message shouldBe "Søknad $søknadId tilhører ikke identen som gjør kallet"
    }

    @Test
    fun `verifiserAtSøknadEksistererOgTilhørerIdent kaster ikke exception hvis søknader eksisterer og tilhører bruker som gjør kallet`() {
        val søknadId = randomUUID()
        søknadRepository.opprett(Søknad(søknadId, ident))

        shouldNotThrow<Exception> {
            søknadRepository.verifiserAtSøknadEksistererOgTilhørerIdent(søknadId, ident)
        }
    }

    @Test
    fun `verifiserAtSøknadHarForventetTilstand kaster exception hvis søknaden ikke eksisterer`() {
        val søknadId = randomUUID()
        val exception =
            shouldThrow<IllegalArgumentException> {
                søknadRepository.verifiserAtSøknadHarForventetTilstand(søknadId, PÅBEGYNT)
            }

        exception.message shouldBe "Fant ikke søknad med ID $søknadId"
    }

    @Test
    fun `verifiserAtSøknadHarForventetTilstand kaster exception hvis søknaden ikke tilhører bruker som gjør kallet`() {
        val søknadId = randomUUID()
        søknadRepository.opprett(Søknad(søknadId, ident, INNSENDT))

        val exception =
            shouldThrow<IllegalStateException> {
                søknadRepository.verifiserAtSøknadHarForventetTilstand(søknadId, PÅBEGYNT)
            }

        exception.message shouldBe "Søknad $søknadId har en annen tilstand (INNSENDT) enn forventet (PÅBEGYNT)"
    }

    @Test
    fun `verifiserAtSøknadHarForventetTilstand kaster ikke exception hvis søknader eksisterer og tilhører bruker som gjør kallet`() {
        val søknadId = randomUUID()
        søknadRepository.opprett(Søknad(søknadId, ident, INNSENDT))

        shouldNotThrow<Exception> {
            søknadRepository.verifiserAtSøknadHarForventetTilstand(søknadId, INNSENDT)
        }
    }

    @Test
    fun `slettSøknadSomSystem oppdaterer søknaden med forventede verdier hvis søknaden eksisterer og tilhører bruker som gjør kallet`() {
        val søknadId = randomUUID()
        val slettetTidspunkt = now().withNano(0)
        søknadRepository.opprett(Søknad(søknadId, ident))

        søknadRepository.slettSøknadSomSystem(søknadId, ident, slettetTidspunkt)

        val søknad = søknadRepository.hent(søknadId)
        søknad shouldNotBe null
        søknad?.tilstand shouldBe SLETTET_AV_SYSTEM
        søknad?.slettetTidspunkt shouldBe slettetTidspunkt
    }

    @Test
    fun `slettSøknadSomSystem kaster exception hvis søknaden ikke eksisterer`() {
        val søknadId = randomUUID()

        val exception =
            shouldThrow<IllegalArgumentException> {
                søknadRepository.slettSøknadSomSystem(søknadId, ident)
            }

        exception.message shouldBe "Fant ikke søknad med ID $søknadId"
    }

    @Test
    fun `slettSøknadSomSystem kaster exception hvis søknaden eksisterer men ikke tilhører bruker som gjør kallet`() {
        val søknadId = randomUUID()
        søknadRepository.opprett(Søknad(søknadId, ident))

        val exception =
            shouldThrow<IllegalArgumentException> {
                søknadRepository.slettSøknadSomSystem(søknadId, "en-annen-ident")
            }

        exception.message shouldBe "Søknad $søknadId tilhører ikke identen som gjør kallet"
    }

    @Test
    fun `hentSoknaderForIdent returnerer tom liste når det ikke finnes noen søknader med seksjoner for gitt ident`() {
        val søknader = søknadRepository.hentSoknaderForIdent(ident)

        søknader shouldBe emptyList()
    }

    @Test
    fun `hentSoknaderForIdent returnerer tom liste når søknad eksisterer men ikke har seksjoner`() {
        val søknadId = randomUUID()
        søknadRepository.opprett(Søknad(søknadId, ident))

        val søknader = søknadRepository.hentSoknaderForIdent(ident)

        søknader shouldBe emptyList()
    }

    @Test
    fun `hentSoknaderForIdent returnerer søknader som har seksjoner for gitt ident`() {
        val søknadId = randomUUID()
        søknadRepository.opprett(Søknad(søknadId, ident))
        seksjonRepository.lagre(søknadId, ident, "seksjon-id", "{}", null, "{}")

        val søknader = søknadRepository.hentSoknaderForIdent(ident)

        søknader.size shouldBe 1
        søknader[0].søknadId shouldBe søknadId
        søknader[0].status shouldBe PÅBEGYNT.name
    }

    @Test
    fun `hentSoknaderForIdent returnerer riktig status og tidspunkter for innsendt søknad`() {
        val søknadId = randomUUID()
        val innsendtTidspunkt = now().withNano(0)
        søknadRepository.opprett(Søknad(søknadId, ident))
        seksjonRepository.lagre(søknadId, ident, "seksjon-id", "{}", null, "{}")
        søknadRepository.markerSøknadSomInnsendt(søknadId, ident, innsendtTidspunkt)

        val søknader = søknadRepository.hentSoknaderForIdent(ident)

        søknader.size shouldBe 1
        søknader[0].søknadId shouldBe søknadId
        søknader[0].status shouldBe INNSENDT.name
        søknader[0].innsendtTimestamp shouldBe innsendtTidspunkt
    }

    @Test
    fun `hentSoknaderForIdent returnerer flere søknader for samme ident`() {
        val søknadId1 = randomUUID()
        val søknadId2 = randomUUID()
        søknadRepository.opprett(Søknad(søknadId1, ident))
        søknadRepository.opprett(Søknad(søknadId2, ident))
        seksjonRepository.lagre(søknadId1, ident, "seksjon-id-1", "{}", null, "{}")
        seksjonRepository.lagre(søknadId2, ident, "seksjon-id-2", "{}", null, "{}")

        val søknader = søknadRepository.hentSoknaderForIdent(ident)

        søknader.size shouldBe 2
        søknader.map { it.søknadId }.shouldContainAll(søknadId1, søknadId2)
    }

    @Test
    fun `hentSoknaderForIdent returnerer ikke søknader som tilhører andre identer`() {
        val søknadIdForIdent = randomUUID()
        val søknadIdForAnnenIdent = randomUUID()
        val annenIdent = "9876543210"
        søknadRepository.opprett(Søknad(søknadIdForIdent, ident))
        søknadRepository.opprett(Søknad(søknadIdForAnnenIdent, annenIdent))
        seksjonRepository.lagre(søknadIdForIdent, ident, "seksjon-id", "{}", null, "{}")
        seksjonRepository.lagre(søknadIdForAnnenIdent, annenIdent, "seksjon-id", "{}", null, "{}")

        val søknader = søknadRepository.hentSoknaderForIdent(ident)

        søknader.size shouldBe 1
        søknader[0].søknadId shouldBe søknadIdForIdent
    }

    @Test
    fun `hentSoknaderForIdent returnerer kun en søknad selv om den har flere seksjoner`() {
        val søknadId = randomUUID()
        søknadRepository.opprett(Søknad(søknadId, ident))
        seksjonRepository.lagre(søknadId, ident, "seksjon-id-1", "{}", null, "{}")
        seksjonRepository.lagre(søknadId, ident, "seksjon-id-2", "{}", null, "{}")
        seksjonRepository.lagre(søknadId, ident, "seksjon-id-3", "{}", null, "{}")

        val søknader = søknadRepository.hentSoknaderForIdent(ident)

        søknader.size shouldBe 1
        søknader[0].søknadId shouldBe søknadId
    }

    private fun opprettSøknad(
        søknadId: UUID,
        tilstand: Tilstand,
        opprettetTidspunkt: LocalDateTime? = null,
        oppdatertTidspunkt: LocalDateTime? = null,
    ): UUID {
        søknadRepository.opprett(Søknad(søknadId, ident, tilstand, oppdatertTidspunkt = oppdatertTidspunkt))
        if (oppdatertTidspunkt != null) {
            søknadRepository.markerSøknadSomOppdatert(søknadId, ident, oppdatertTidspunkt)
        }
        if (opprettetTidspunkt != null) {
            transaction {
                TransactionManager
                    .current()
                    .connection
                    .prepareStatement(
                        "UPDATE soknad SET opprettet='$opprettetTidspunkt' WHERE soknad_id='$søknadId'",
                        arrayOf(),
                    ).executeUpdate()
            }
        }
        return søknadId
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
