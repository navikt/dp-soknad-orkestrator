package no.nav.dagpenger.soknad.orkestrator.søknad.seksjon

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.equals.shouldNotBeEqual
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
    private val seksjonsvar = "{\"key\": \"value\"}"
    private val seksjonsvar2 = "{\"key2\": \"value2\"}"
    private val seksjonsvar3 = "{\"key3\": \"value3\"}"
    private val pdfGrunnlag = "{\"pdfGrunnlagKey\": \"pdfGrunnlagValue\"}"
    private val pdfGrunnlag2 = "{\"pdfGrunnlagKey2\": \"pdfGrunnlagValue2\"}"
    private val pdfGrunnlag3 = "{\"pdfGrunnlagKey3\": \"pdfGrunnlagValue3\"}"
    private val seksjonId = "seksjon-id"
    private val seksjonId2 = "seksjon-id-2"
    private val seksjonId3 = "seksjon-id-3"
    private val dokumentasjonskrav = "{\"dokumentasjonskravKey\": \"dokumentasjonskravValue\"}"
    private val dokumentasjonskrav2 = "{\"dokumentasjonskravKey2\": \"dokumentasjonskravValue2\"}"

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
        søknadRepository.opprett(Søknad(søknadId, ident))

        shouldNotThrowAny {
            seksjonRepository.lagre(ident, søknadId, seksjonId, seksjonsvar, dokumentasjonskrav, pdfGrunnlag)
        }
        seksjonRepository.hentSeksjonsvar(ident, søknadId, seksjonId) shouldBe seksjonsvar
    }

    @Test
    @Suppress("ktlint:standard:max-line-length")
    fun `lagre kaster ingen exception hvis seksjonen som lagres er uten dokumentasjonskrav og tilhører en søknad som tilhører bruker som gjør kallet`() {
        val søknadId = randomUUID()
        søknadRepository.opprett(Søknad(søknadId, ident))

        shouldNotThrowAny {
            seksjonRepository.lagre(
                ident = ident,
                søknadId = søknadId,
                seksjonId = seksjonId,
                seksjonsvar = seksjonsvar,
                pdfGrunnlag = pdfGrunnlag,
            )
        }
        seksjonRepository.hentSeksjonsvar(ident, søknadId, seksjonId) shouldBe seksjonsvar
    }

    @Test
    fun `lagre kaster exception hvis seksjonen lagres på en søknad som ikke eksisterer`() {
        val lagretSøknadId = randomUUID()
        val requestSøknadId = randomUUID()
        søknadRepository.opprett(Søknad(lagretSøknadId, ident))

        val exception =
            shouldThrow<IllegalArgumentException> {
                seksjonRepository.lagre(ident, requestSøknadId, seksjonId, seksjonsvar, dokumentasjonskrav, pdfGrunnlag)
            }

        exception.message shouldBe "Fant ikke søknad med ID $requestSøknadId"
    }

    @Test
    fun `lagre kaster exception hvis seksjonen lagres på en søknad som tilhører en annen bruker enn den som gjør kallet`() {
        val søknadId = randomUUID()
        søknadRepository.opprett(Søknad(søknadId, ident))

        val exception =
            shouldThrow<IllegalArgumentException> {
                seksjonRepository.lagre(
                    "en-annen-ident",
                    søknadId,
                    seksjonId,
                    seksjonsvar,
                    dokumentasjonskrav,
                    pdfGrunnlag,
                )
            }

        exception.message shouldBe "Søknad $søknadId tilhører ikke identen som gjør kallet"
    }

    @Test
    fun `lagre gjør UPDATE hvis gitt søknadId og seksjonId eksisterer`() {
        val søknadId = randomUUID()
        søknadRepository.opprett(Søknad(søknadId, ident))
        seksjonRepository.lagre(ident, søknadId, seksjonId, seksjonsvar, dokumentasjonskrav, pdfGrunnlag)
        seksjonRepository.lagre(ident, søknadId, seksjonId, seksjonsvar2, dokumentasjonskrav2, pdfGrunnlag2)

        seksjonRepository.hentSeksjonsvar(ident, søknadId, seksjonId) shouldBe seksjonsvar2
    }

    @Test
    fun `lagre gjør UPDATE hvis gitt søknadId og seksjonId eksisterer og dokumentasjonskrav er null`() {
        val søknadId = randomUUID()
        søknadRepository.opprett(Søknad(søknadId, ident))
        seksjonRepository.lagre(ident, søknadId, seksjonId, seksjonsvar, dokumentasjonskrav, pdfGrunnlag)
        seksjonRepository.lagre(ident, søknadId, seksjonId, seksjonsvar2, null, pdfGrunnlag2)

        seksjonRepository.hentDokumentasjonskrav(ident, søknadId, seksjonId) shouldBe null
    }

    @Test
    fun `hentSeksjonsvar returnerer forventet seksjon hvis seksjonen tilhører en søknad som tilhører bruker som gjør kallet`() {
        val søknadId = randomUUID()
        søknadRepository.opprett(Søknad(søknadId, ident))
        seksjonRepository.lagre(ident, søknadId, seksjonId, seksjonsvar, dokumentasjonskrav, pdfGrunnlag)

        seksjonRepository.hentSeksjonsvar(ident, søknadId, seksjonId) shouldBe seksjonsvar
    }

    @Test
    fun `hentSeksjonsvar returnerer null hvis seksjonen tilhører en søknad som tilhører en annen bruker enn den som gjør kallet`() {
        val søknadId = randomUUID()
        søknadRepository.opprett(Søknad(søknadId, ident))
        seksjonRepository.lagre(ident, søknadId, seksjonId, seksjonsvar, dokumentasjonskrav, pdfGrunnlag)

        seksjonRepository.hentSeksjonsvar("en-annen-ident", søknadId, seksjonId) shouldBe null
    }

    @Test
    fun `hentSeksjonsvar returnerer null hvis seksjonen tilhører en søknad som ikke eksisterer`() {
        val søknadId = randomUUID()
        søknadRepository.opprett(Søknad(søknadId, ident))
        seksjonRepository.lagre(ident, søknadId, seksjonId, seksjonsvar, dokumentasjonskrav, pdfGrunnlag)

        seksjonRepository.hentSeksjonsvar(ident, randomUUID(), seksjonId) shouldBe null
    }

    @Test
    fun `hentSeksjonsvarEllerKastException kaster exception hvis ingen seksjoner er lagret i databasen`() {
        val søknadId = randomUUID()
        søknadRepository.opprett(Søknad(søknadId, ident))

        val exception =
            shouldThrow<IllegalStateException> {
                seksjonRepository.hentSeksjonsvarEllerKastException(ident, søknadId, seksjonId)
            }

        exception.message shouldBe "Fant ingen seksjonsvar på $seksjonId for søknad $søknadId"
    }

    @Test
    fun `hentSeksjoner returnerer forventede seksjoner hvis søknaden tilhører bruker som gjør kallet`() {
        val søknadId = randomUUID()
        søknadRepository.opprett(Søknad(søknadId, ident))
        seksjonRepository.lagre(ident, søknadId, seksjonId, seksjonsvar, dokumentasjonskrav, pdfGrunnlag)
        seksjonRepository.lagre(ident, søknadId, seksjonId2, seksjonsvar2, dokumentasjonskrav2, pdfGrunnlag2)

        val seksjoner = seksjonRepository.hentSeksjoner(ident, søknadId)

        seksjoner shouldContainExactlyInAnyOrder
            listOf(
                Seksjon(seksjonId, seksjonsvar),
                Seksjon(seksjonId2, seksjonsvar2),
            )
    }

    @Test
    fun `hentSeksjoner returnerer tom liste hvis søknaden tilhører en annen bruker enn den som gjør kallet`() {
        val søknadId = randomUUID()
        søknadRepository.opprett(Søknad(søknadId, ident))
        seksjonRepository.lagre(ident, søknadId, seksjonId, seksjonsvar, dokumentasjonskrav, pdfGrunnlag)

        seksjonRepository.hentSeksjoner("en-annen-ident", søknadId) shouldBe emptyList()
    }

    @Test
    fun `hentSeksjoner returnerer tom liste hvis søknaden ikke eksisterer`() {
        val søknadId = randomUUID()
        søknadRepository.opprett(Søknad(søknadId, ident))
        seksjonRepository.lagre(ident, søknadId, seksjonId, seksjonsvar, dokumentasjonskrav, pdfGrunnlag)

        seksjonRepository.hentSeksjoner(ident, randomUUID()) shouldBe emptyList()
    }

    @Test
    @Suppress("ktlint:standard:max-line-length")
    fun `hentSeksjonIdForAlleLagredeSeksjoner returnerer forventede seksjoner hvis søknaden eksisterer og tilhører bruker som gjør kallet`() {
        val søknadId = randomUUID()
        søknadRepository.opprett(Søknad(søknadId, ident))
        seksjonRepository.lagre(ident, søknadId, seksjonId, seksjonsvar, dokumentasjonskrav, pdfGrunnlag)
        seksjonRepository.lagre(ident, søknadId, seksjonId2, seksjonsvar, dokumentasjonskrav2, pdfGrunnlag2)

        val seksjoner = seksjonRepository.hentSeksjonIdForAlleLagredeSeksjoner(ident, søknadId)

        seksjoner shouldContainExactlyInAnyOrder listOf(seksjonId, seksjonId2)
    }

    @Test
    @Suppress("ktlint:standard:max-line-length")
    fun `hentSeksjonIdForAlleLagredeSeksjoner returnerer tom liste hvis søknaden eksisterer, men tilhører en annen bruker enn den som gjør kallet`() {
        val søknadId = randomUUID()
        søknadRepository.opprett(Søknad(søknadId, ident))
        seksjonRepository.lagre(ident, søknadId, seksjonId, seksjonsvar, dokumentasjonskrav, pdfGrunnlag)

        val seksjoner = seksjonRepository.hentSeksjonIdForAlleLagredeSeksjoner("en-annen-ident", søknadId)

        seksjoner shouldBe emptyList()
    }

    @Test
    fun `hentSeksjonIdForAlleLagredeSeksjoner returnerer tom liste hvis søknaden ikke eksisterer`() {
        val søknadId = randomUUID()
        søknadRepository.opprett(Søknad(søknadId, ident))
        seksjonRepository.lagre(ident, søknadId, seksjonId, seksjonsvar, dokumentasjonskrav, pdfGrunnlag)

        seksjonRepository.hentSeksjonIdForAlleLagredeSeksjoner(ident, randomUUID()) shouldBe emptyList()
    }

    @Test
    fun `søknadRepository sin slett() sletter alle seksjoner for en gitt søknadId i seksjonstabellen`() {
        val søknadId = randomUUID()
        val søknadId2 = randomUUID()

        søknadRepository.opprett(Søknad(søknadId, ident))
        søknadRepository.opprett(Søknad(søknadId2, ident))
        seksjonRepository.lagre(ident, søknadId, seksjonId, seksjonsvar, dokumentasjonskrav, pdfGrunnlag)
        seksjonRepository.lagre(ident, søknadId, seksjonId2, seksjonsvar2, dokumentasjonskrav, pdfGrunnlag2)
        seksjonRepository.lagre(ident, søknadId2, seksjonId, seksjonsvar, dokumentasjonskrav, pdfGrunnlag)

        val seksjonerFørSletting = seksjonRepository.hentSeksjoner(ident, søknadId)
        seksjonerFørSletting shouldNotBeEqual emptyList()

        søknadRepository.slett(søknadId, ident)
        val seksjonerEtterSletting = seksjonRepository.hentSeksjoner(ident, søknadId)
        seksjonerEtterSletting shouldBe emptyList()

        val seksjonerPåAnnenSøknad = seksjonRepository.hentSeksjoner(ident, søknadId2)
        seksjonerPåAnnenSøknad shouldNotBeEqual emptyList()

        søknadRepository.slett(søknadId2, ident)
        val seksjonerEtterSlettingAnnenSøknad = seksjonRepository.hentSeksjoner(ident, søknadId2)
        seksjonerEtterSlettingAnnenSøknad shouldBe emptyList()
    }

    @Test
    @Suppress("ktlint:standard:max-line-length")
    fun `lagreDokumentasjonskrav kaster lagrer forventet dokumentasjonskrav hvis input ikke er null og seksjonen det lagres på tilhører en søknad som tilhører bruker som gjør kallet`() {
        val søknadId = randomUUID()
        søknadRepository.opprett(Søknad(søknadId, ident))
        seksjonRepository.lagre(ident, søknadId, seksjonId, seksjonsvar, dokumentasjonskrav, pdfGrunnlag)

        shouldNotThrowAny {
            seksjonRepository.lagreDokumentasjonskrav(ident, søknadId, seksjonId, dokumentasjonskrav2)
        }

        seksjonRepository.hentDokumentasjonskrav(ident, søknadId, seksjonId) shouldBe dokumentasjonskrav2
    }

    @Test
    @Suppress("ktlint:standard:max-line-length")
    fun `lagreDokumentasjonskrav kaster lagrer forventet dokumentasjonskrav hvis input er null og seksjonen det lagres på tilhører en søknad som tilhører bruker som gjør kallet`() {
        val søknadId = randomUUID()
        søknadRepository.opprett(Søknad(søknadId, ident))
        seksjonRepository.lagre(ident, søknadId, seksjonId, seksjonsvar, dokumentasjonskrav, pdfGrunnlag)

        shouldNotThrowAny {
            seksjonRepository.lagreDokumentasjonskrav(ident, søknadId, seksjonId, null)
        }

        seksjonRepository.hentDokumentasjonskrav(ident, søknadId, seksjonId) shouldBe null
    }

    @Test
    fun `lagreDokumentasjonskrav kaster exception hvis seksjonen det lagres på tilhører til en søknad som ikke eksisterer`() {
        val lagretSøknadId = randomUUID()
        val requestSøknadId = randomUUID()
        søknadRepository.opprett(Søknad(lagretSøknadId, ident))
        seksjonRepository.lagre(ident, lagretSøknadId, seksjonId, seksjonsvar, dokumentasjonskrav, pdfGrunnlag)

        val exception =
            shouldThrow<IllegalArgumentException> {
                seksjonRepository.lagreDokumentasjonskrav(ident, requestSøknadId, seksjonId, dokumentasjonskrav2)
            }

        exception.message shouldBe "Fant ikke søknad med ID $requestSøknadId"
    }

    @Test
    @Suppress("ktlint:standard:max-line-length")
    fun `lagreDokumentasjonskrav kaster exception hvis seksjonen det lagres på tilhører en søknad som tilhører en annen bruker enn den som gjør kallet`() {
        val søknadId = randomUUID()
        søknadRepository.opprett(Søknad(søknadId, ident))
        seksjonRepository.lagre(ident, søknadId, seksjonId, seksjonsvar, dokumentasjonskrav, pdfGrunnlag)

        val exception =
            shouldThrow<IllegalArgumentException> {
                seksjonRepository.lagreDokumentasjonskrav("en-annen-ident", søknadId, seksjonId, dokumentasjonskrav2)
            }

        exception.message shouldBe "Søknad $søknadId tilhører ikke identen som gjør kallet"
    }

    @Test
    @Suppress("ktlint:standard:max-line-length")
    fun `lagreDokumentasjonskrav kaster exception hvis seksjonen ikke eksisterer`() {
        val søknadId = randomUUID()
        søknadRepository.opprett(Søknad(søknadId, ident))
        seksjonRepository.lagre(ident, søknadId, seksjonId, seksjonsvar, dokumentasjonskrav, pdfGrunnlag)

        val exception =
            shouldThrow<IllegalArgumentException> {
                seksjonRepository.lagreDokumentasjonskrav(
                    ident,
                    søknadId,
                    "en-annen-seksjon-id",
                    dokumentasjonskrav2,
                )
            }

        exception.message shouldBe "Fant ikke seksjon med ID en-annen-seksjon-id"
    }

    @Test
    fun `hentDokumentasjonskrav returnerer forventede seksjoner hvis søknaden tilhører bruker som gjør kallet`() {
        val søknadId = randomUUID()
        søknadRepository.opprett(Søknad(søknadId, ident))
        seksjonRepository.lagre(ident, søknadId, seksjonId, seksjonsvar, dokumentasjonskrav, pdfGrunnlag)
        seksjonRepository.lagre(ident, søknadId, seksjonId2, seksjonsvar2, dokumentasjonskrav2, pdfGrunnlag2)
        seksjonRepository.lagre(ident, søknadId, seksjonId3, seksjonsvar3, null, pdfGrunnlag3)

        val dokumentasjonskravForSøknad = seksjonRepository.hentDokumentasjonskrav(ident, søknadId)

        dokumentasjonskravForSøknad shouldContainExactlyInAnyOrder
            listOf(
                dokumentasjonskrav,
                dokumentasjonskrav2,
            )
    }

    @Test
    fun `hentDokumentasjonskrav returnerer tom liste hvis søknaden tilhører en annen bruker enn den som gjør kallet`() {
        val søknadId = randomUUID()
        søknadRepository.opprett(Søknad(søknadId, ident))
        seksjonRepository.lagre(ident, søknadId, seksjonId, seksjonsvar, dokumentasjonskrav, pdfGrunnlag)

        seksjonRepository.hentDokumentasjonskrav("en-annen-ident", søknadId) shouldBe emptyList()
    }

    @Test
    fun `hentDokumentasjonskrav returnerer tom liste hvis søknaden ikke eksisterer`() {
        val søknadId = randomUUID()
        søknadRepository.opprett(Søknad(søknadId, ident))
        seksjonRepository.lagre(ident, søknadId, seksjonId, seksjonsvar, dokumentasjonskrav, pdfGrunnlag)

        seksjonRepository.hentDokumentasjonskrav(ident, randomUUID()) shouldBe emptyList()
    }

    @Test
    fun `hentPdfGrunnlag returnerer forventede seksjoner hvis søknaden tilhører bruker som gjør kallet`() {
        val søknadId = randomUUID()
        søknadRepository.opprett(Søknad(søknadId, ident))
        seksjonRepository.lagre(ident, søknadId, seksjonId, seksjonsvar, dokumentasjonskrav, pdfGrunnlag)
        seksjonRepository.lagre(ident, søknadId, seksjonId2, seksjonsvar2, dokumentasjonskrav2, pdfGrunnlag2)

        val dokumentasjonskravForSøknad = seksjonRepository.hentPdfGrunnlag(ident, søknadId)

        dokumentasjonskravForSøknad shouldContainExactly
            listOf(
                pdfGrunnlag,
                pdfGrunnlag2,
            )
    }

    @Test
    fun `hentPdfGrunnlag returnerer tom liste hvis søknaden tilhører en annen bruker enn den som gjør kallet`() {
        val søknadId = randomUUID()
        søknadRepository.opprett(Søknad(søknadId, ident))
        seksjonRepository.lagre(ident, søknadId, seksjonId, seksjonsvar, dokumentasjonskrav, pdfGrunnlag)

        seksjonRepository.hentPdfGrunnlag("en-annen-ident", søknadId) shouldBe emptyList()
    }

    @Test
    fun `hentPdfGrunnlag returnerer tom liste hvis søknaden ikke eksisterer`() {
        val søknadId = randomUUID()
        søknadRepository.opprett(Søknad(søknadId, ident))
        seksjonRepository.lagre(ident, søknadId, seksjonId, seksjonsvar, dokumentasjonskrav, pdfGrunnlag)

        seksjonRepository.hentPdfGrunnlag(ident, randomUUID()) shouldBe emptyList()
    }

    @Test
    fun `slettAlleSeksjoner sletter forventede seksjoner hvis søknaden tilhører bruker som gjør kallet`() {
        val søknadId1 = randomUUID()
        val søknadId2 = randomUUID()
        søknadRepository.opprett(Søknad(søknadId1, ident))
        søknadRepository.opprett(Søknad(søknadId2, ident))
        seksjonRepository.lagre(ident, søknadId1, seksjonId, seksjonsvar, dokumentasjonskrav, pdfGrunnlag)
        seksjonRepository.lagre(ident, søknadId1, "seksjonId2", seksjonsvar, dokumentasjonskrav, pdfGrunnlag)
        seksjonRepository.lagre(ident, søknadId2, seksjonId, seksjonsvar, dokumentasjonskrav, pdfGrunnlag)
        seksjonRepository.lagre(ident, søknadId2, "seksjonId2", seksjonsvar, dokumentasjonskrav, pdfGrunnlag)

        seksjonRepository.slettAlleSeksjoner(ident, søknadId1)

        seksjonRepository.hentSeksjoner(ident, søknadId1) shouldBe emptyList()
        seksjonRepository.hentSeksjoner(ident, søknadId2).size shouldBe 2
    }

    @Test
    fun `slettAlleSeksjoner kaster exception hvis søknaden ikke eksisterer`() {
        val søknadId = randomUUID()

        val exception =
            shouldThrow<IllegalArgumentException> {
                seksjonRepository.slettAlleSeksjoner("en-annen-ident", søknadId)
            }

        exception.message shouldBe "Fant ikke søknad med ID $søknadId"
    }

    @Test
    fun `slettAlleSeksjoner kaster exception hvis søknaden ikke tilhører bruker som gjør kallet`() {
        val søknadId = randomUUID()
        søknadRepository.opprett(Søknad(søknadId, ident))

        val exception =
            shouldThrow<IllegalArgumentException> {
                seksjonRepository.slettAlleSeksjoner("en-annen-ident", søknadId)
            }

        exception.message shouldBe "Søknad $søknadId tilhører ikke identen som gjør kallet"
    }
}
