package no.nav.dagpenger.soknad.orkestrator.søknad.seksjon

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import no.nav.dagpenger.soknad.orkestrator.db.Postgres.dataSource
import no.nav.dagpenger.soknad.orkestrator.db.Postgres.withMigratedDb
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.Søknad
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import java.util.UUID.randomUUID
import kotlin.test.BeforeTest
import kotlin.test.Test

class SeksjonRepositoryTest {
    private lateinit var seksjonRepository: SeksjonRepository
    private lateinit var søknadRepository: SøknadRepository

    private val ident = "1234567890"
    private val json = "{\"key\": \"value\"}"
    private val json2 = "{\"key2\": \"value2\"}"
    private val seksjonId = "seksjon-id"
    private val seksjonId2 = "seksjon-id-2"

    @BeforeTest
    fun setup() {
        withMigratedDb {
            søknadRepository = SøknadRepository(dataSource, mockk<QuizOpplysningRepository>(relaxed = true))
            seksjonRepository =
                SeksjonRepository(
                    dataSource,
                    søknadRepository,
                )
        }
    }

    @Test
    fun `lagre kaster ingen exception hvis seksjonen som lagres tilhører en søknad som tilhører bruker som gjør kallet`() {
        val søknadId = randomUUID()
        søknadRepository.lagre(Søknad(søknadId, ident))

        shouldNotThrowAny {
            seksjonRepository.lagre(ident, søknadId, seksjonId, json)
        }
    }

    @Test
    fun `lagre kaster exception hvis seksjonen lagres på en søknad som ikke eksisterer`() {
        val søknadId = randomUUID()
        søknadRepository.lagre(Søknad(søknadId, ident))

        shouldThrow<IllegalArgumentException> {
            seksjonRepository.lagre(ident, randomUUID(), seksjonId, json)
        }
    }

    @Test
    fun `lagre kaster exception hvis seksjonen lagres på en søknad som tilhører en annen bruker enn den som gjør kallet`() {
        val søknadId = randomUUID()
        søknadRepository.lagre(Søknad(søknadId, ident))

        shouldThrow<IllegalArgumentException> {
            seksjonRepository.lagre("en-annen-ident", søknadId, seksjonId, json)
        }
    }

    @Test
    fun `hent returnerer forventet seksjon hvis seksjonen tilhører en søknad som tilhører bruker som gjør kallet`() {
        val søknadId = randomUUID()
        søknadRepository.lagre(Søknad(søknadId, ident))
        seksjonRepository.lagre(ident, søknadId, seksjonId, json)

        seksjonRepository.hent(ident, søknadId, seksjonId) shouldBe json
    }

    @Test
    fun `hent returnerer null hvis seksjonen tilhører en søknad som tilhører en annen bruker enn den som gjør kallet`() {
        val søknadId = randomUUID()
        søknadRepository.lagre(Søknad(søknadId, ident))
        seksjonRepository.lagre(ident, søknadId, seksjonId, json)

        seksjonRepository.hent("en-annen-ident", søknadId, seksjonId) shouldBe null
    }

    @Test
    fun `hent returnerer null hvis seksjonen tilhører en søknad som ikke eksisterer`() {
        val søknadId = randomUUID()
        søknadRepository.lagre(Søknad(søknadId, ident))
        seksjonRepository.lagre(ident, søknadId, seksjonId, json)

        seksjonRepository.hent(ident, randomUUID(), seksjonId) shouldBe null
    }

    @Test
    fun `hentSeksjoner returnerer forventede seksjoner hvis søknaden tilhører bruker som gjør kallet`() {
        val søknadId = randomUUID()
        søknadRepository.lagre(Søknad(søknadId, ident))
        seksjonRepository.lagre(ident, søknadId, seksjonId, json)
        seksjonRepository.lagre(ident, søknadId, seksjonId2, json2)

        val seksjoner = seksjonRepository.hentSeksjoner(ident, søknadId)

        seksjoner shouldContainExactlyInAnyOrder
            listOf(
                Seksjon(seksjonId, json),
                Seksjon(seksjonId2, json2),
            )
    }

    @Test
    fun `hentSeksjoner returnerer tom liste hvis søknaden tilhører en annen bruker enn den som gjør kallet`() {
        val søknadId = randomUUID()
        søknadRepository.lagre(Søknad(søknadId, ident))
        seksjonRepository.lagre(ident, søknadId, seksjonId, json)

        seksjonRepository.hentSeksjoner("en-annen-ident", søknadId) shouldBe emptyList()
    }

    @Test
    fun `hentSeksjoner returnerer tom liste hvis søknaden ikke eksisterer`() {
        val søknadId = randomUUID()
        søknadRepository.lagre(Søknad(søknadId, ident))
        seksjonRepository.lagre(ident, søknadId, seksjonId, json)

        seksjonRepository.hentSeksjoner(ident, randomUUID()) shouldBe emptyList()
    }

    @Test
    fun `hentFullførteSeksjoner returnerer forventede seksjoner hvis søknaden eksisterer og tilhører bruker som gjør kallet`() {
        val søknadId = randomUUID()
        søknadRepository.lagre(Søknad(søknadId, ident))
        seksjonRepository.lagre(ident, søknadId, seksjonId, json)
        seksjonRepository.lagre(ident, søknadId, seksjonId2, json)

        val seksjoner = seksjonRepository.hentFullførteSeksjoner(ident, søknadId)

        seksjoner shouldContainExactlyInAnyOrder listOf(seksjonId, seksjonId2)
    }

    @Test
    fun `hentFullførteSeksjoner returnerer tom liste hvis søknaden eksisterer, men tilhører en annen bruker enn den som gjør kallet`() {
        val søknadId = randomUUID()
        søknadRepository.lagre(Søknad(søknadId, ident))
        seksjonRepository.lagre(ident, søknadId, seksjonId, json)

        val seksjoner = seksjonRepository.hentFullførteSeksjoner("en-annen-ident", søknadId)

        seksjoner shouldBe emptyList()
    }

    @Test
    fun `hentFullførteSeksjoner returnerer tom liste hvis søknaden ikke eksisterer`() {
        val søknadId = randomUUID()
        søknadRepository.lagre(Søknad(søknadId, ident))
        seksjonRepository.lagre(ident, søknadId, seksjonId, json)

        seksjonRepository.hentFullførteSeksjoner(ident, randomUUID()) shouldBe emptyList()
    }
}
