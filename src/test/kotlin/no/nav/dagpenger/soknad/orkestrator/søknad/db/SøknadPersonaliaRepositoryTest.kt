package no.nav.dagpenger.soknad.orkestrator.søknad.db

import io.kotest.matchers.shouldBe
import io.mockk.mockk
import no.nav.dagpenger.soknad.orkestrator.db.Postgres.dataSource
import no.nav.dagpenger.soknad.orkestrator.db.Postgres.withMigratedDb
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.Søknad
import no.nav.dagpenger.soknad.orkestrator.søknad.SøknadPersonalia
import java.util.UUID
import java.util.UUID.randomUUID
import kotlin.test.BeforeTest
import kotlin.test.Test

class SøknadPersonaliaRepositoryTest {
    private lateinit var søknadRepository: SøknadRepository
    private lateinit var søknadPersonaliaRepository: SøknadPersonaliaRepository

    private val ident = "10216290386"

    @BeforeTest
    fun setup() {
        withMigratedDb {
            søknadRepository = SøknadRepository(dataSource, mockk<QuizOpplysningRepository>(relaxed = true))
            søknadPersonaliaRepository = SøknadPersonaliaRepository(dataSource)
        }
    }

    @Test
    fun `lagre setter inn en ny rad hvis ingen rad eksisterer fra før for søknadId og ident`() {
        val søknadId = randomUUID()
        val forventetSøknadPersonalia = lagSøknadPersonalia(søknadId)
        søknadRepository.opprett(Søknad(søknadId, ident))

        søknadPersonaliaRepository.lagre(forventetSøknadPersonalia)

        søknadPersonaliaRepository.hent(søknadId, ident) shouldBe forventetSøknadPersonalia
    }

    @Test
    fun `lagre håndterer lagring av personalia med tillatte null-verdier`() {
        val søknadId = randomUUID()
        val forventetSøknadPersonalia =
            SøknadPersonalia(søknadId, ident, fornavn = "fornavn", etternavn = "etternavn", alder = "26")
        søknadRepository.opprett(Søknad(søknadId, ident))

        søknadPersonaliaRepository.lagre(forventetSøknadPersonalia)

        søknadPersonaliaRepository.hent(søknadId, ident) shouldBe forventetSøknadPersonalia
    }

    @Test
    fun `lagre oppdaterer en eksisterende rad hvis en rad allerede eksisterer for søknadId og ident, men andre rader oppdateres ikke`() {
        val søknadIdSomSkalOppdateres = randomUUID()
        val søknadIdSomIkkeSkalOppdateres = randomUUID()
        val søknadPersonaliaSomSkalOppdateres = lagSøknadPersonalia(søknadIdSomSkalOppdateres)
        val søknadPersonaliaSomIkkeSkalOppdateres = lagSøknadPersonalia(søknadIdSomIkkeSkalOppdateres)
        søknadRepository.opprett(Søknad(søknadIdSomSkalOppdateres, ident))
        søknadRepository.opprett(Søknad(søknadIdSomIkkeSkalOppdateres, ident))
        søknadPersonaliaRepository.lagre(søknadPersonaliaSomSkalOppdateres)
        søknadPersonaliaRepository.lagre(søknadPersonaliaSomIkkeSkalOppdateres)
        val søknadPersonaliaSomErOppdatert = søknadPersonaliaSomSkalOppdateres.copy(fornavn = "nyttFornavn")

        søknadPersonaliaRepository.lagre(søknadPersonaliaSomErOppdatert)

        søknadPersonaliaRepository.hent(søknadIdSomSkalOppdateres, ident) shouldBe søknadPersonaliaSomErOppdatert
        søknadPersonaliaRepository.hent(
            søknadIdSomIkkeSkalOppdateres,
            ident,
        ) shouldBe søknadPersonaliaSomIkkeSkalOppdateres
    }

    @Test
    fun `hent returnerer forventet resultat hvis søknadPersonalia for søknadId og ident eksisterer`() {
        val søknadId = randomUUID()
        val forventetSøknadPersonalia = lagSøknadPersonalia(søknadId)
        søknadRepository.opprett(Søknad(søknadId, ident))

        søknadPersonaliaRepository.lagre(forventetSøknadPersonalia)

        søknadPersonaliaRepository.hent(søknadId, ident) shouldBe forventetSøknadPersonalia
    }

    @Test
    fun `hent returnerer null hvis søknadPersonalia for søknadId ikke eksisterer`() {
        søknadPersonaliaRepository.hent(randomUUID(), ident) shouldBe null
    }

    @Test
    fun `hent returnerer null hvis søknadPersonalia for kombinasjon av søknadId og ident ikke eksisterer`() {
        val søknadId = randomUUID()
        søknadRepository.opprett(Søknad(søknadId, ident))
        søknadPersonaliaRepository.lagre(lagSøknadPersonalia(søknadId))

        søknadPersonaliaRepository.hent(randomUUID(), "en-annen-ident") shouldBe null
    }

    @Test
    fun `hent returnerer null hvis søknadPersonalia for søknadId ikke eksisterer, men ident eksisterer`() {
        val søknadId = randomUUID()
        søknadRepository.opprett(Søknad(søknadId, ident))
        søknadPersonaliaRepository.lagre(lagSøknadPersonalia(søknadId))

        søknadPersonaliaRepository.hent(randomUUID(), ident) shouldBe null
    }

    private fun lagSøknadPersonalia(søknadId: UUID) =
        SøknadPersonalia(
            søknadId,
            ident,
            "fornavn",
            "mellomnavn",
            "etternavn",
            "67",
            "adresselinje1",
            "adresselinje2",
            "adresselinje3",
            "4321",
            "poststed",
            "LAN",
            "land",
            "kontonummer",
        )
}
