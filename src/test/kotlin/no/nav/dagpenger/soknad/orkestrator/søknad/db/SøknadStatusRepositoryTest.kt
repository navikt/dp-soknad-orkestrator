package no.nav.dagpenger.soknad.orkestrator.søknad.db

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.mockk
import no.nav.dagpenger.soknad.orkestrator.db.Postgres.dataSource
import no.nav.dagpenger.soknad.orkestrator.db.Postgres.withMigratedDb
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.Status
import no.nav.dagpenger.soknad.orkestrator.søknad.Status.Avslag
import no.nav.dagpenger.soknad.orkestrator.søknad.Status.Innvilgelse
import no.nav.dagpenger.soknad.orkestrator.søknad.Søknad
import no.nav.dagpenger.soknad.orkestrator.søknad.SøknadStatus
import org.jetbrains.exposed.exceptions.ExposedSQLException
import java.util.UUID.randomUUID
import kotlin.test.BeforeTest
import kotlin.test.Test

class SøknadStatusRepositoryTest {
    private lateinit var søknadStatusRepository: SøknadStatusRepository
    private lateinit var søknadRepository: SøknadRepository

    private val ident = "1234567890"
    private val behandlingId = "3fa85f64-5717-4562-b3fc-2c963f66afa6"

    private val rettighetsperioder =
        """[{"fraOgMed":"2026-09-08","tilOgMed":"2026-09-08","harRett":true,"opprinnelse":"Ny"}]"""
    private val rettighetsperioder2 =
        """[{"fraOgMed":"2026-10-01","tilOgMed":null,"harRett":false,"opprinnelse":"Endring"}]"""

    @BeforeTest
    fun setup() {
        withMigratedDb {
            søknadRepository = SøknadRepository(dataSource, mockk<QuizOpplysningRepository>(relaxed = true))
            søknadStatusRepository = SøknadStatusRepository(dataSource)
        }
    }

    @Test
    fun `lagre kaster ingen exception hvis søknadstatusen som lagres tilhører en eksisterende søknad`() {
        val søknadId = randomUUID()
        søknadRepository.opprett(Søknad(søknadId, ident))

        shouldNotThrowAny {
            søknadStatusRepository.lagre(SøknadStatus(søknadId, ident, behandlingId, Innvilgelse, rettighetsperioder))
        }
        søknadStatusRepository.hent(søknadId) shouldNotBe null
    }

    @Test
    fun `lagre kaster exception hvis søknaden ikke eksisterer`() {
        shouldThrow<ExposedSQLException> {
            søknadStatusRepository.lagre(
                SøknadStatus(randomUUID(), ident, behandlingId, Innvilgelse, rettighetsperioder),
            )
        }
    }

    @Test
    fun `lagre kaster exception hvis rettighetsperioder ikke er gyldig json`() {
        val søknadId = randomUUID()
        søknadRepository.opprett(Søknad(søknadId, ident))

        shouldThrow<ExposedSQLException> {
            søknadStatusRepository.lagre(SøknadStatus(søknadId, ident, behandlingId, Innvilgelse, "ikke json"))
        }
    }

    @Test
    fun `hent returnerer søknadstatusen som ble lagret`() {
        val søknadId = randomUUID()
        val søknadStatus = SøknadStatus(søknadId, ident, behandlingId, Innvilgelse, rettighetsperioder)
        søknadRepository.opprett(Søknad(søknadId, ident))

        søknadStatusRepository.lagre(søknadStatus)

        søknadStatusRepository.hent(søknadId) shouldBe søknadStatus
    }

    @Test
    fun `hent returnerer nyeste søknadstatus siden lagring er append-only`() {
        val søknadId = randomUUID()
        val førsteStatus = SøknadStatus(søknadId, ident, behandlingId, Avslag, rettighetsperioder)
        val nyesteStatus = SøknadStatus(søknadId, ident, behandlingId, Innvilgelse, rettighetsperioder2)
        søknadRepository.opprett(Søknad(søknadId, ident))

        søknadStatusRepository.lagre(førsteStatus)
        søknadStatusRepository.lagre(nyesteStatus)

        søknadStatusRepository.hent(søknadId) shouldBe nyesteStatus
    }

    @Test
    fun `hent returnerer bare søknadstatus for søknaden det spørres om`() {
        val søknadId = randomUUID()
        val annenSøknadId = randomUUID()
        val søknadStatus = SøknadStatus(søknadId, ident, behandlingId, Avslag, rettighetsperioder)
        val annenSøknadStatus = SøknadStatus(annenSøknadId, ident, behandlingId, Innvilgelse, rettighetsperioder2)
        søknadRepository.opprett(Søknad(søknadId, ident))
        søknadRepository.opprett(Søknad(annenSøknadId, ident))

        søknadStatusRepository.lagre(søknadStatus)
        søknadStatusRepository.lagre(annenSøknadStatus)

        søknadStatusRepository.hent(søknadId) shouldBe søknadStatus
        søknadStatusRepository.hent(annenSøknadId) shouldBe annenSøknadStatus
    }

    @Test
    fun `hent returnerer null hvis det ikke finnes søknadstatus for søknadId`() {
        søknadStatusRepository.hent(randomUUID()) shouldBe null
    }

    @Test
    fun `lagre og hent håndterer alle verdiene i Status`() {
        Status.entries.forEach { status ->
            val søknadId = randomUUID()
            val søknadStatus = SøknadStatus(søknadId, ident, behandlingId, status, rettighetsperioder)
            søknadRepository.opprett(Søknad(søknadId, ident))

            søknadStatusRepository.lagre(søknadStatus)

            søknadStatusRepository.hent(søknadId)?.førteTil shouldBe status
        }
    }

    @Test
    fun `lagre og hent håndterer tom liste med rettighetsperioder`() {
        val søknadId = randomUUID()
        val søknadStatus = SøknadStatus(søknadId, ident, behandlingId, Innvilgelse, "[]")
        søknadRepository.opprett(Søknad(søknadId, ident))

        søknadStatusRepository.lagre(søknadStatus)

        søknadStatusRepository.hent(søknadId)?.rettighetsperioder shouldBe "[]"
    }
}
