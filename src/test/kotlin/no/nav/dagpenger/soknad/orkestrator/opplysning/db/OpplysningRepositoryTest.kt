package no.nav.dagpenger.soknad.orkestrator.opplysning.db

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.date.shouldBeAfter
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.mockk
import no.nav.dagpenger.soknad.orkestrator.db.Postgres.dataSource
import no.nav.dagpenger.soknad.orkestrator.db.Postgres.withMigratedDb
import no.nav.dagpenger.soknad.orkestrator.opplysning.BooleanSvar
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysning
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysningstype.BOOLEAN
import no.nav.dagpenger.soknad.orkestrator.opplysning.Svar
import no.nav.dagpenger.soknad.orkestrator.søknad.Søknad
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test

class OpplysningRepositoryTest {
    private lateinit var søknadRepository: SøknadRepository
    private lateinit var opplysningRepository: OpplysningRepository

    @BeforeTest
    fun setup() {
        withMigratedDb {
            søknadRepository =
                SøknadRepository(
                    dataSource = dataSource,
                    quizOpplysningRepository = mockk(),
                )
            opplysningRepository = OpplysningRepository(dataSource)
        }
    }

    @Test
    fun `Lagring av seksjon feiler hvis søknad ikke er lagret`() {
        val søknad = Søknad(UUID.randomUUID(), "1234567890")
        val seksjonversjon = "SEKSJON_V1"

        shouldThrow<IllegalStateException> {
            opplysningRepository.opprettSeksjon(søknad.søknadId, seksjonversjon)
        }.message shouldBe "Fant ikke søknad med id ${søknad.søknadId}"
    }

    @Test
    fun `Kan lagre og hente opplysning`() {
        val søknad = Søknad(UUID.randomUUID(), "1234567890")
        val opplysning = lagOpplysning()

        søknadRepository.lagre(søknad)
        opplysningRepository.opprettSeksjon(søknad.søknadId, opplysning.seksjonversjon)
        opplysningRepository.lagre(søknad.søknadId, opplysning)
        val hentetOpplysning = opplysningRepository.hent(opplysning.opplysningId)

        hentetOpplysning?.opplysningId shouldBe opplysning.opplysningId
        hentetOpplysning?.seksjonversjon shouldBe opplysning.seksjonversjon
        hentetOpplysning?.opplysningsbehovId shouldBe opplysning.opplysningsbehovId
        hentetOpplysning?.type shouldBe opplysning.type
        hentetOpplysning?.svar?.verdi shouldBe opplysning.svar?.verdi
    }

    @Test
    fun `sistEndretAvBruker er null når en ny opplysning lagres`() {
        val søknad = Søknad(UUID.randomUUID(), "1234567890")
        val opplysning = lagOpplysning()

        søknadRepository.lagre(søknad)
        opplysningRepository.opprettSeksjon(søknad.søknadId, opplysning.seksjonversjon)
        opplysningRepository.lagre(søknad.søknadId, opplysning)

        sistEndretAvBruker(opplysning.opplysningId) shouldBe null
    }

    @Test
    fun `Lagring av opplysning feiler hvis seksjon ikke er lagret`() {
        val søknad = Søknad(UUID.randomUUID(), "1234567890")
        val opplysning = lagOpplysning()

        søknadRepository.lagre(søknad)

        shouldThrow<IllegalStateException> {
            opplysningRepository.lagre(søknad.søknadId, opplysning)
        }.message shouldBe "Fant ikke seksjon med søknadId ${søknad.søknadId}"
    }

    @Test
    fun `Lagring av opplysning feiler hvis søknad ikke finnes`() {
        val søknadId = UUID.randomUUID()
        val opplysning = lagOpplysning()

        shouldThrow<IllegalStateException> {
            opplysningRepository.lagre(søknadId, opplysning)
        }.message shouldBe "Fant ikke søknad med id $søknadId"
    }

    @Test
    fun `lagreSvar legger til svar på eksisterende opplysning`() {
        val søknad = Søknad(UUID.randomUUID(), "1234567890")
        val opplysning = lagOpplysning()

        søknadRepository.lagre(søknad)
        opplysningRepository.opprettSeksjon(søknad.søknadId, opplysning.seksjonversjon)
        opplysningRepository.lagre(søknad.søknadId, opplysning)
        val hentetOpplysning = opplysningRepository.hent(opplysning.opplysningId)

        hentetOpplysning?.svar shouldBe null

        opplysningRepository.lagreSvar(BooleanSvar(opplysning.opplysningId, true))
        val oppdatertOpplysning = opplysningRepository.hent(opplysning.opplysningId)

        oppdatertOpplysning?.svar?.verdi shouldBe true
    }

    @Test
    fun `lagreSvar oppdaterer sistEndretAvBruker i databasen`() {
        val søknad = Søknad(UUID.randomUUID(), "1234567890")
        val opplysning = lagOpplysning()

        søknadRepository.lagre(søknad)
        opplysningRepository.opprettSeksjon(søknad.søknadId, opplysning.seksjonversjon)
        opplysningRepository.lagre(søknad.søknadId, opplysning)
        opplysningRepository.lagreSvar(BooleanSvar(opplysning.opplysningId, true))
        val opprinneligSistEndretAvBruker = sistEndretAvBruker(opplysning.opplysningId)

        opprinneligSistEndretAvBruker shouldNotBe null

        opplysningRepository.lagreSvar(BooleanSvar(opplysning.opplysningId, false))
        val nySistEndretAvBruker = sistEndretAvBruker(opplysning.opplysningId)

        nySistEndretAvBruker!! shouldBeAfter opprinneligSistEndretAvBruker!!
    }

    @Test
    fun `lagreSvar kaster exception hvis opplysning ikke finnes`() {
        val opplysningId = UUID.randomUUID()

        shouldThrow<IllegalStateException> {
            opplysningRepository.lagreSvar(BooleanSvar(opplysningId, true))
        }.message shouldBe "Fant ikke opplysning med id $opplysningId, kan ikke lagre svar"
    }

    @Test
    fun `hentAlle returnerer alle opplysninger for en gitt søknadId`() {
        val søknad = Søknad(UUID.randomUUID(), "1234567890")
        val opplysning1 = lagOpplysning(seksjonversjon = "SEKSJON_V1")
        val opplysning2 = lagOpplysning(seksjonversjon = "SEKSJON_V2")

        søknadRepository.lagre(søknad)
        opplysningRepository.opprettSeksjon(søknad.søknadId, opplysning1.seksjonversjon)
        opplysningRepository.opprettSeksjon(søknad.søknadId, opplysning2.seksjonversjon)
        opplysningRepository.lagre(søknad.søknadId, opplysning1)
        opplysningRepository.lagre(søknad.søknadId, opplysning2)

        val opplysninger = opplysningRepository.hentAlle(søknad.søknadId)

        opplysninger.size shouldBe 2
        opplysninger[0].opplysningId shouldBe opplysning1.opplysningId
        opplysninger[1].opplysningId shouldBe opplysning2.opplysningId
    }

    @Test
    fun `hentAlle returnerer tom liste hvis ingen opplysninger finnes for en gitt søknadId`() {
        val søknad = Søknad(UUID.randomUUID(), "1234567890")
        søknadRepository.lagre(søknad)

        val opplysninger = opplysningRepository.hentAlle(søknad.søknadId)
        opplysninger shouldBe emptyList()
    }

    @Test
    fun `hentAlle kaster exception hvis det ikke finnes søknad med gitt id`() {
        val søknadId = UUID.randomUUID()

        shouldThrow<IllegalStateException> {
            opplysningRepository.hentAlle(søknadId)
        }.message shouldBe "Fant ikke søknad med id $søknadId"
    }

    @Test
    fun `hentAlleForSeksjon returnerer alle opplysninger for gitt seksjonversjon`() {
        val søknad = Søknad(UUID.randomUUID(), "1234567890")
        val opplysning1 = lagOpplysning(seksjonversjon = "SEKSJON_V1")
        val opplysning2 = lagOpplysning(seksjonversjon = "ANNEN_SEKSJON_V1")

        søknadRepository.lagre(søknad)
        opplysningRepository.opprettSeksjon(søknad.søknadId, opplysning1.seksjonversjon)
        opplysningRepository.opprettSeksjon(søknad.søknadId, opplysning2.seksjonversjon)
        opplysningRepository.lagre(søknad.søknadId, opplysning1)
        opplysningRepository.lagre(søknad.søknadId, opplysning2)

        val opplysninger = opplysningRepository.hentAlleForSeksjon(søknad.søknadId, opplysning1.seksjonversjon)

        opplysninger.size shouldBe 1
        opplysninger[0].opplysningId shouldBe opplysning1.opplysningId
    }

    @Test
    fun `hentAlleForSeksjon returnerer tom liste hvis ingen opplysninger finnes for gitt seksjonversjon`() {
        val søknad = Søknad(UUID.randomUUID(), "1234567890")
        val seksjonversjon = "SEKSJON_V1"
        val opplysning = lagOpplysning(seksjonversjon = "ANNEN_SEKSJON_V1")

        søknadRepository.lagre(søknad)
        opplysningRepository.opprettSeksjon(søknad.søknadId, seksjonversjon)
        opplysningRepository.opprettSeksjon(søknad.søknadId, opplysning.seksjonversjon)
        opplysningRepository.lagre(søknad.søknadId, opplysning)

        val opplysninger = opplysningRepository.hentAlleForSeksjon(søknad.søknadId, seksjonversjon)

        opplysninger shouldBe emptyList()
    }

    @Test
    fun `hentAlleForSeksjon kaster feil hvis søknad ikke finnes`() {
        val søknadId = UUID.randomUUID()
        val seksjonversjon = "SEKSJON_V1"

        shouldThrow<IllegalStateException> {
            opplysningRepository.hentAlleForSeksjon(søknadId, seksjonversjon)
        }.message shouldBe "Fant ikke søknad med id $søknadId, kan ikke hente opplysninger"
    }

    @Test
    fun `hentAlleForSeksjon kaster feil hvis seksjon ikke finnes`() {
        val søknad = Søknad(UUID.randomUUID(), "1234567890")
        val seksjonversjon = "SEKSJON_V1"

        søknadRepository.lagre(søknad)

        shouldThrow<IllegalStateException> {
            opplysningRepository.hentAlleForSeksjon(søknad.søknadId, seksjonversjon)
        }.message shouldBe "Fant ikke seksjon med versjon $seksjonversjon for søknad med id ${søknad.søknadId}, kan ikke hente opplysninger"
    }

    @Test
    fun `kan slette opplysning basert på søknadId, seksjonversjon og opplysningsbehovId`() {
        val søknad = Søknad(UUID.randomUUID(), "1234567890")
        val seksjonversjon = "SEKSJON_V1"
        val opplysning = lagOpplysning(seksjonversjon = seksjonversjon, opplysningbehovId = 1)
        val opplysning2 = lagOpplysning(seksjonversjon = seksjonversjon, opplysningbehovId = 2)

        søknadRepository.lagre(søknad)
        opplysningRepository.opprettSeksjon(søknad.søknadId, seksjonversjon)
        opplysningRepository.lagre(søknad.søknadId, opplysning)
        opplysningRepository.lagre(søknad.søknadId, opplysning2)

        opplysningRepository.slett(søknad.søknadId, seksjonversjon, opplysning.opplysningsbehovId)

        val opplysninger = opplysningRepository.hentAlle(søknad.søknadId)
        opplysninger.size shouldBe 1
        opplysninger.none { it.opplysningId == opplysning.opplysningId } shouldBe true
    }

    @Test
    fun `slett gjør ingenting hvis opplysning ikke finnes`() {
        val søknad = Søknad(UUID.randomUUID(), "1234567890")
        val seksjonversjon = "SEKSJON_V1"

        søknadRepository.lagre(søknad)
        opplysningRepository.opprettSeksjon(søknad.søknadId, seksjonversjon)

        shouldNotThrowAny {
            opplysningRepository.slett(søknad.søknadId, seksjonversjon, 999)
        }
    }

    @Test
    fun `slett kaster exception hvis søknad ikke finnes`() {
        val søknadId = UUID.randomUUID()
        val seksjonversjon = "SEKSJON_V1"

        shouldThrow<IllegalStateException> {
            opplysningRepository.slett(søknadId, seksjonversjon, 1)
        }.message shouldBe "Fant ikke søknad med id $søknadId, kan ikke slette opplysning"
    }

    @Test
    fun `slett kaster exception hvis seksjon ikke finnes`() {
        val søknad = Søknad(UUID.randomUUID(), "1234567890")
        val seksjonversjon = "SEKSJON_V1"

        søknadRepository.lagre(søknad)

        shouldThrow<IllegalStateException> {
            opplysningRepository.slett(søknad.søknadId, seksjonversjon, 1)
        }.message shouldBe "Fant ikke seksjon med versjon $seksjonversjon for søknad med id ${søknad.søknadId}, kan ikke slette opplysning"
    }

    private fun lagOpplysning(
        seksjonversjon: String = "SEKSJON_V1",
        opplysningbehovId: Int = 1,
        svar: Svar<*>? = null,
    ) = Opplysning(
        opplysningId = UUID.randomUUID(),
        seksjonversjon = seksjonversjon,
        opplysningsbehovId = opplysningbehovId,
        type = svar?.type ?: BOOLEAN,
        svar = svar,
    )

    private fun sistEndretAvBruker(opplysningId: UUID?) =
        transaction {
            OpplysningTabell.selectAll()
                .single { it[OpplysningTabell.opplysningId] == opplysningId }[OpplysningTabell.sistEndretAvBruker]
        }
}
