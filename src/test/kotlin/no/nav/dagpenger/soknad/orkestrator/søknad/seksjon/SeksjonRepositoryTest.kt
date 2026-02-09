package no.nav.dagpenger.soknad.orkestrator.søknad.seksjon

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.equals.shouldNotBeEqual
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import no.nav.dagpenger.soknad.orkestrator.db.Postgres.dataSource
import no.nav.dagpenger.soknad.orkestrator.db.Postgres.withMigratedDb
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.Søknad
import no.nav.dagpenger.soknad.orkestrator.søknad.Tilstand.INNSENDT
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import java.time.LocalDateTime.now
import java.util.UUID.randomUUID
import kotlin.test.BeforeTest
import kotlin.test.Test

class SeksjonRepositoryTest {
    private lateinit var seksjonRepository: SeksjonRepository
    private lateinit var søknadRepository: SøknadRepository
    private val seksjonId = "seksjon-id"
    private val seksjonId2 = "seksjon-id-2"
    private val seksjonId3 = "seksjon-id-3"

    private val ident = "1234567890"
    val seksjonsvarJsonForLagring =
        """
        {
            "seksjonId":"din-situasjon",
            "seksjonsvar":{
                "key": "value"
            },
            "versjon":1
        }
        """.trimIndent()
    val seksjonSvarObjekt = objectMapper.readTree("""{"key": "value"}""")

    private val seksjonsvar2JsonForLagring =
        """
        {
            "seksjonId":"din-situasjon",
            "seksjonsvar":{
                "key2": "value2"
            },
            "versjon":1
        }
        """.trimIndent()
    val seksjonSvar2Objekt = objectMapper.readTree("""{"key2": "value2"}""")

    private val seksjonsvar3JsonForLagring =
        """
        {
            "seksjonId":"din-situasjon",
            "seksjonsvar":{
                "key3": "value3"
            },
            "versjon":1
        }
        """.trimIndent()
    val seksjonSvar3Objekt = objectMapper.readTree("""{"key3": "value3"}""")

    private val pdfGrunnlag = "{\"pdfGrunnlagKey\": \"pdfGrunnlagValue\"}"
    private val pdfGrunnlag2 = "{\"pdfGrunnlagKey2\": \"pdfGrunnlagValue2\"}"
    private val pdfGrunnlag3 = "{\"pdfGrunnlagKey3\": \"pdfGrunnlagValue3\"}"

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
            seksjonRepository.lagre(
                søknadId,
                ident,
                seksjonId,
                seksjonsvarJsonForLagring,
                dokumentasjonskrav,
                pdfGrunnlag,
            )
        }
        val seksjonssvar = seksjonRepository.hentSeksjonsvar(søknadId, ident, seksjonId)
        println(seksjonssvar)
        seksjonssvar shouldBe
            SeksjonsvarDAO(seksjonId = "din-situasjon", seksjonsvar = seksjonSvarObjekt, versjon = 1)
    }

    @Test
    @Suppress("ktlint:standard:max-line-length")
    fun `lagre kaster ingen exception hvis seksjonen som lagres er uten dokumentasjonskrav og tilhører en søknad som tilhører bruker som gjør kallet`() {
        val søknadId = randomUUID()
        søknadRepository.opprett(Søknad(søknadId, ident))

        shouldNotThrowAny {
            seksjonRepository.lagre(
                søknadId = søknadId,
                ident = ident,
                seksjonId = seksjonId,
                seksjonsvar = seksjonsvarJsonForLagring,
                pdfGrunnlag = pdfGrunnlag,
            )
        }
        seksjonRepository.hentSeksjonsvar(søknadId, ident, seksjonId) shouldBe
            SeksjonsvarDAO(seksjonId = "din-situasjon", seksjonsvar = seksjonSvarObjekt, versjon = 1)
    }

    @Test
    fun `lagre kaster exception hvis seksjonen lagres på en søknad som ikke eksisterer`() {
        val lagretSøknadId = randomUUID()
        val requestSøknadId = randomUUID()
        søknadRepository.opprett(Søknad(lagretSøknadId, ident))

        val exception =
            shouldThrow<IllegalArgumentException> {
                seksjonRepository.lagre(
                    requestSøknadId,
                    ident,
                    seksjonId,
                    seksjonsvarJsonForLagring,
                    dokumentasjonskrav,
                    pdfGrunnlag,
                )
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
                    søknadId,
                    "en-annen-ident",
                    seksjonId,
                    seksjonsvarJsonForLagring,
                    dokumentasjonskrav,
                    pdfGrunnlag,
                )
            }

        exception.message shouldBe "Søknad $søknadId tilhører ikke identen som gjør kallet"
    }

    @Test
    fun `lagre kaster exception hvis seksjonen lagres på en søknad som er i en annen tilstand enn PÅBEGYNT`() {
        val søknadId = randomUUID()
        søknadRepository.opprett(Søknad(søknadId, ident, INNSENDT))

        val exception =
            shouldThrow<IllegalStateException> {
                seksjonRepository.lagre(
                    søknadId,
                    ident,
                    seksjonId,
                    seksjonsvarJsonForLagring,
                    dokumentasjonskrav,
                    pdfGrunnlag,
                )
            }

        exception.message shouldBe "Søknad $søknadId har en annen tilstand (INNSENDT) enn forventet (PÅBEGYNT)"
    }

    @Test
    fun `lagre gjør UPDATE hvis gitt søknadId og seksjonId eksisterer`() {
        val søknadId = randomUUID()
        søknadRepository.opprett(Søknad(søknadId, ident))
        seksjonRepository.lagre(søknadId, ident, seksjonId, seksjonsvarJsonForLagring, dokumentasjonskrav, pdfGrunnlag)
        seksjonRepository.lagre(
            søknadId,
            ident,
            seksjonId,
            seksjonsvar2JsonForLagring,
            dokumentasjonskrav2,
            pdfGrunnlag2,
        )

        seksjonRepository.hentSeksjonsvar(søknadId, ident, seksjonId) shouldBe
            SeksjonsvarDAO(seksjonId = "din-situasjon", seksjonsvar = seksjonSvar2Objekt, versjon = 1)
    }

    @Test
    fun `lagre gjør UPDATE hvis gitt søknadId og seksjonId eksisterer og dokumentasjonskrav er null`() {
        val søknadId = randomUUID()
        søknadRepository.opprett(Søknad(søknadId, ident))
        seksjonRepository.lagre(søknadId, ident, seksjonId, seksjonsvarJsonForLagring, dokumentasjonskrav, pdfGrunnlag)
        seksjonRepository.lagre(søknadId, ident, seksjonId, seksjonsvar2JsonForLagring, pdfGrunnlag = pdfGrunnlag2)

        seksjonRepository.hentDokumentasjonskrav(søknadId, ident, seksjonId) shouldBe null
    }

    @Test
    fun `hentSeksjonsvar returnerer forventet seksjon hvis seksjonen tilhører en søknad som tilhører bruker som gjør kallet`() {
        val søknadId = randomUUID()
        søknadRepository.opprett(Søknad(søknadId, ident))
        seksjonRepository.lagre(søknadId, ident, seksjonId, seksjonsvarJsonForLagring, dokumentasjonskrav, pdfGrunnlag)

        seksjonRepository.hentSeksjonsvar(søknadId, ident, seksjonId) shouldBe
            SeksjonsvarDAO(seksjonId = "din-situasjon", seksjonsvar = seksjonSvarObjekt, versjon = 1)
    }

    @Test
    fun `hentSeksjonsvar returnerer null hvis seksjonen tilhører en søknad som tilhører en annen bruker enn den som gjør kallet`() {
        val søknadId = randomUUID()
        søknadRepository.opprett(Søknad(søknadId, ident))
        seksjonRepository.lagre(søknadId, ident, seksjonId, seksjonsvarJsonForLagring, dokumentasjonskrav, pdfGrunnlag)

        seksjonRepository.hentSeksjonsvar(søknadId, "en-annen-ident", seksjonId) shouldBe null
    }

    @Test
    fun `hentSeksjonsvar returnerer null hvis seksjonen tilhører en søknad som ikke eksisterer`() {
        val søknadId = randomUUID()
        søknadRepository.opprett(Søknad(søknadId, ident))
        seksjonRepository.lagre(søknadId, ident, seksjonId, seksjonsvarJsonForLagring, dokumentasjonskrav, pdfGrunnlag)

        seksjonRepository.hentSeksjonsvar(randomUUID(), ident, seksjonId) shouldBe null
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
        seksjonRepository.lagre(søknadId, ident, seksjonId, seksjonsvarJsonForLagring, dokumentasjonskrav, pdfGrunnlag)
        seksjonRepository.lagre(
            søknadId,
            ident,
            seksjonId2,
            seksjonsvar2JsonForLagring,
            dokumentasjonskrav2,
            pdfGrunnlag2,
        )

        val seksjoner = seksjonRepository.hentSeksjoner(søknadId, ident)

        seksjoner shouldContainExactlyInAnyOrder
            listOf(
                Seksjon(seksjonId, SeksjonsvarDAO(seksjonId = "din-situasjon", seksjonsvar = seksjonSvarObjekt, versjon = 1)),
                Seksjon(seksjonId2, SeksjonsvarDAO(seksjonId = "din-situasjon", seksjonsvar = seksjonSvar2Objekt, versjon = 1)),
            )
    }

    @Test
    fun `hentSeksjoner returnerer tom liste hvis søknaden tilhører en annen bruker enn den som gjør kallet`() {
        val søknadId = randomUUID()
        søknadRepository.opprett(Søknad(søknadId, ident))
        seksjonRepository.lagre(søknadId, ident, seksjonId, seksjonsvarJsonForLagring, dokumentasjonskrav, pdfGrunnlag)

        seksjonRepository.hentSeksjoner(søknadId, "en-annen-ident") shouldBe emptyList()
    }

    @Test
    fun `hentSeksjoner returnerer tom liste hvis søknaden ikke eksisterer`() {
        val søknadId = randomUUID()
        søknadRepository.opprett(Søknad(søknadId, ident))
        seksjonRepository.lagre(søknadId, ident, seksjonId, seksjonsvarJsonForLagring, dokumentasjonskrav, pdfGrunnlag)

        seksjonRepository.hentSeksjoner(randomUUID(), ident) shouldBe emptyList()
    }

    @Test
    @Suppress("ktlint:standard:max-line-length")
    fun `hentSeksjonIdForAlleLagredeSeksjoner returnerer forventede seksjoner hvis søknaden eksisterer og tilhører bruker som gjør kallet`() {
        val søknadId = randomUUID()
        søknadRepository.opprett(Søknad(søknadId, ident))
        seksjonRepository.lagre(søknadId, ident, seksjonId, seksjonsvarJsonForLagring, dokumentasjonskrav, pdfGrunnlag)
        seksjonRepository.lagre(
            søknadId,
            ident,
            seksjonId2,
            seksjonsvarJsonForLagring,
            dokumentasjonskrav2,
            pdfGrunnlag2,
        )

        val seksjoner = seksjonRepository.hentSeksjonIdForAlleLagredeSeksjoner(søknadId, ident)

        seksjoner shouldContainExactlyInAnyOrder listOf(seksjonId, seksjonId2)
    }

    @Test
    @Suppress("ktlint:standard:max-line-length")
    fun `hentSeksjonIdForAlleLagredeSeksjoner returnerer tom liste hvis søknaden eksisterer, men tilhører en annen bruker enn den som gjør kallet`() {
        val søknadId = randomUUID()
        søknadRepository.opprett(Søknad(søknadId, ident))
        seksjonRepository.lagre(søknadId, ident, seksjonId, seksjonsvarJsonForLagring, dokumentasjonskrav, pdfGrunnlag)

        val seksjoner = seksjonRepository.hentSeksjonIdForAlleLagredeSeksjoner(søknadId, "en-annen-ident")

        seksjoner shouldBe emptyList()
    }

    @Test
    fun `hentSeksjonIdForAlleLagredeSeksjoner returnerer tom liste hvis søknaden ikke eksisterer`() {
        val søknadId = randomUUID()
        søknadRepository.opprett(Søknad(søknadId, ident))
        seksjonRepository.lagre(søknadId, ident, seksjonId, seksjonsvarJsonForLagring, dokumentasjonskrav, pdfGrunnlag)

        seksjonRepository.hentSeksjonIdForAlleLagredeSeksjoner(randomUUID(), ident) shouldBe emptyList()
    }

    @Test
    fun `søknadRepository sin slett() sletter alle seksjoner for en gitt søknadId i seksjonstabellen`() {
        val søknadId = randomUUID()
        val søknadId2 = randomUUID()

        søknadRepository.opprett(Søknad(søknadId, ident))
        søknadRepository.opprett(Søknad(søknadId2, ident))
        seksjonRepository.lagre(søknadId, ident, seksjonId, seksjonsvarJsonForLagring, dokumentasjonskrav, pdfGrunnlag)
        seksjonRepository.lagre(
            søknadId,
            ident,
            seksjonId2,
            seksjonsvar2JsonForLagring,
            dokumentasjonskrav,
            pdfGrunnlag2,
        )
        seksjonRepository.lagre(søknadId2, ident, seksjonId, seksjonsvarJsonForLagring, dokumentasjonskrav, pdfGrunnlag)

        val seksjonerFørSletting = seksjonRepository.hentSeksjoner(søknadId, ident)
        seksjonerFørSletting shouldNotBeEqual emptyList()

        søknadRepository.slett(søknadId, ident)
        val seksjonerEtterSletting = seksjonRepository.hentSeksjoner(søknadId, ident)
        seksjonerEtterSletting shouldBe emptyList()

        val seksjonerPåAnnenSøknad = seksjonRepository.hentSeksjoner(søknadId2, ident)
        seksjonerPåAnnenSøknad shouldNotBeEqual emptyList()

        søknadRepository.slett(søknadId2, ident)
        val seksjonerEtterSlettingAnnenSøknad = seksjonRepository.hentSeksjoner(søknadId2, ident)
        seksjonerEtterSlettingAnnenSøknad shouldBe emptyList()
    }

    @Test
    @Suppress("ktlint:standard:max-line-length")
    fun `lagreDokumentasjonskrav kaster lagrer forventet dokumentasjonskrav hvis input ikke er null og seksjonen det lagres på tilhører en søknad som tilhører bruker som gjør kallet`() {
        val søknadId = randomUUID()
        val søknadId2 = randomUUID()
        søknadRepository.opprett(Søknad(søknadId, ident))
        seksjonRepository.lagre(søknadId, ident, seksjonId, seksjonsvarJsonForLagring, dokumentasjonskrav, pdfGrunnlag)
        søknadRepository.opprett(Søknad(søknadId2, ident))
        seksjonRepository.lagre(søknadId2, ident, seksjonId, seksjonsvarJsonForLagring, dokumentasjonskrav, pdfGrunnlag)

        shouldNotThrowAny {
            seksjonRepository.lagreDokumentasjonskrav(søknadId, ident, seksjonId, dokumentasjonskrav2)
        }

        seksjonRepository.hentDokumentasjonskrav(søknadId, ident, seksjonId) shouldBe dokumentasjonskrav2
        seksjonRepository.hentDokumentasjonskrav(søknadId2, ident, seksjonId) shouldBe dokumentasjonskrav
    }

    @Test
    @Suppress("ktlint:standard:max-line-length")
    fun `lagreDokumentasjonskrav kaster lagrer forventet dokumentasjonskrav hvis input er null og seksjonen det lagres på tilhører en søknad som tilhører bruker som gjør kallet`() {
        val søknadId = randomUUID()
        søknadRepository.opprett(Søknad(søknadId, ident))
        seksjonRepository.lagre(søknadId, ident, seksjonId, seksjonsvarJsonForLagring, dokumentasjonskrav, pdfGrunnlag)

        shouldNotThrowAny {
            seksjonRepository.lagreDokumentasjonskrav(søknadId, ident, seksjonId, null)
        }

        seksjonRepository.hentDokumentasjonskrav(søknadId, ident, seksjonId) shouldBe null
    }

    @Test
    fun `lagreDokumentasjonskrav kaster exception hvis seksjonen det lagres på tilhører til en søknad som ikke eksisterer`() {
        val lagretSøknadId = randomUUID()
        val requestSøknadId = randomUUID()
        søknadRepository.opprett(Søknad(lagretSøknadId, ident))
        seksjonRepository.lagre(
            lagretSøknadId,
            ident,
            seksjonId,
            seksjonsvarJsonForLagring,
            dokumentasjonskrav,
            pdfGrunnlag,
        )

        val exception =
            shouldThrow<IllegalArgumentException> {
                seksjonRepository.lagreDokumentasjonskrav(requestSøknadId, ident, seksjonId, dokumentasjonskrav2)
            }

        exception.message shouldBe "Fant ikke søknad med ID $requestSøknadId"
    }

    @Test
    @Suppress("ktlint:standard:max-line-length")
    fun `lagreDokumentasjonskrav kaster exception hvis seksjonen det lagres på tilhører en søknad som tilhører en annen bruker enn den som gjør kallet`() {
        val søknadId = randomUUID()
        søknadRepository.opprett(Søknad(søknadId, ident))
        seksjonRepository.lagre(søknadId, ident, seksjonId, seksjonsvarJsonForLagring, dokumentasjonskrav, pdfGrunnlag)

        val exception =
            shouldThrow<IllegalArgumentException> {
                seksjonRepository.lagreDokumentasjonskrav(søknadId, "en-annen-ident", seksjonId, dokumentasjonskrav2)
            }

        exception.message shouldBe "Søknad $søknadId tilhører ikke identen som gjør kallet"
    }

    @Test
    @Suppress("ktlint:standard:max-line-length")
    fun `lagreDokumentasjonskrav kaster exception hvis seksjonen det lagres på tilhører en søknad som er i en annen tilstand en PÅBEGYNT`() {
        val søknadId = randomUUID()
        søknadRepository.opprett(Søknad(søknadId, ident))
        seksjonRepository.lagre(søknadId, ident, seksjonId, seksjonsvarJsonForLagring, dokumentasjonskrav, pdfGrunnlag)
        søknadRepository.markerSøknadSomInnsendt(søknadId, ident, now())

        val exception =
            shouldThrow<IllegalStateException> {
                seksjonRepository.lagreDokumentasjonskrav(søknadId, ident, seksjonId, dokumentasjonskrav2)
            }

        exception.message shouldBe "Søknad $søknadId har en annen tilstand (INNSENDT) enn forventet (PÅBEGYNT)"
    }

    @Test
    @Suppress("ktlint:standard:max-line-length")
    fun `lagreDokumentasjonskravEttersending kaster lagrer forventet dokumentasjonskrav hvis input ikke er null og seksjonen det lagres på tilhører en søknad som tilhører bruker som gjør kallet`() {
        val søknadId = randomUUID()
        søknadRepository.opprett(Søknad(søknadId, ident))
        seksjonRepository.lagre(søknadId, ident, seksjonId, seksjonsvarJsonForLagring, dokumentasjonskrav, pdfGrunnlag)
        søknadRepository.markerSøknadSomInnsendt(søknadId, ident, now())

        shouldNotThrowAny {
            seksjonRepository.lagreDokumentasjonskravEttersending(søknadId, ident, seksjonId, dokumentasjonskrav2)
        }

        seksjonRepository.hentDokumentasjonskrav(søknadId, ident, seksjonId) shouldBe dokumentasjonskrav2
    }

    @Test
    @Suppress("ktlint:standard:max-line-length")
    fun `lagreDokumentasjonskravEttersending kaster lagrer forventet dokumentasjonskrav hvis input er null og seksjonen det lagres på tilhører en søknad som tilhører bruker som gjør kallet`() {
        val søknadId = randomUUID()
        søknadRepository.opprett(Søknad(søknadId, ident))
        seksjonRepository.lagre(søknadId, ident, seksjonId, seksjonsvarJsonForLagring, dokumentasjonskrav, pdfGrunnlag)
        søknadRepository.markerSøknadSomJournalført(søknadId, ident, "journalPostId", now())

        shouldNotThrowAny {
            seksjonRepository.lagreDokumentasjonskravEttersending(søknadId, ident, seksjonId, null)
        }

        seksjonRepository.hentDokumentasjonskrav(søknadId, ident, seksjonId) shouldBe null
    }

    @Test
    fun `lagreDokumentasjonskravEttersending kaster exception hvis seksjonen det lagres på tilhører til en søknad som ikke eksisterer`() {
        val lagretSøknadId = randomUUID()
        val requestSøknadId = randomUUID()
        søknadRepository.opprett(Søknad(lagretSøknadId, ident))
        seksjonRepository.lagre(
            lagretSøknadId,
            ident,
            seksjonId,
            seksjonsvarJsonForLagring,
            dokumentasjonskrav,
            pdfGrunnlag,
        )
        søknadRepository.markerSøknadSomInnsendt(lagretSøknadId, ident, now())

        val exception =
            shouldThrow<IllegalArgumentException> {
                seksjonRepository.lagreDokumentasjonskravEttersending(
                    requestSøknadId,
                    ident,
                    seksjonId,
                    dokumentasjonskrav2,
                )
            }

        exception.message shouldBe "Fant ikke søknad med ID $requestSøknadId"
    }

    @Test
    @Suppress("ktlint:standard:max-line-length")
    fun `lagreDokumentasjonskravEttersending kaster exception hvis seksjonen det lagres på tilhører en søknad som tilhører en annen bruker enn den som gjør kallet`() {
        val søknadId = randomUUID()
        søknadRepository.opprett(Søknad(søknadId, ident))
        seksjonRepository.lagre(søknadId, ident, seksjonId, seksjonsvarJsonForLagring, dokumentasjonskrav, pdfGrunnlag)
        søknadRepository.markerSøknadSomInnsendt(søknadId, ident, now())

        val exception =
            shouldThrow<IllegalArgumentException> {
                seksjonRepository.lagreDokumentasjonskravEttersending(
                    søknadId,
                    "en-annen-ident",
                    seksjonId,
                    dokumentasjonskrav2,
                )
            }

        exception.message shouldBe "Søknad $søknadId tilhører ikke identen som gjør kallet"
    }

    @Test
    @Suppress("ktlint:standard:max-line-length")
    fun `lagreDokumentasjonskrav kaster exception hvis seksjonen det lagres på tilhører en søknad som er i tilstanden PÅBEGYNT`() {
        val søknadId = randomUUID()
        søknadRepository.opprett(Søknad(søknadId, ident))
        seksjonRepository.lagre(søknadId, ident, seksjonId, seksjonsvarJsonForLagring, dokumentasjonskrav, pdfGrunnlag)

        val exception =
            shouldThrow<IllegalStateException> {
                seksjonRepository.lagreDokumentasjonskravEttersending(søknadId, ident, seksjonId, dokumentasjonskrav2)
            }

        exception.message shouldBe "Søknad $søknadId har en annen tilstand (PÅBEGYNT) enn forventet ([INNSENDT, JOURNALFØRT])"
    }

    @Test
    @Suppress("ktlint:standard:max-line-length")
    fun `lagreDokumentasjonskrav kaster exception hvis seksjonen ikke eksisterer`() {
        val søknadId = randomUUID()
        søknadRepository.opprett(Søknad(søknadId, ident))
        seksjonRepository.lagre(søknadId, ident, seksjonId, seksjonsvarJsonForLagring, dokumentasjonskrav, pdfGrunnlag)

        val exception =
            shouldThrow<IllegalArgumentException> {
                seksjonRepository.lagreDokumentasjonskrav(
                    søknadId,
                    ident,
                    "en-annen-seksjon-id",
                    dokumentasjonskrav2,
                )
            }

        exception.message shouldBe "Fant ikke seksjon med ID en-annen-seksjon-id"
    }

    @Test
    fun `hentDokumentasjonskrav returnerer forventede dokumentasjonskrav hvis søknaden tilhører bruker som gjør kallet`() {
        val søknadId = randomUUID()
        søknadRepository.opprett(Søknad(søknadId, ident))
        seksjonRepository.lagre(søknadId, ident, seksjonId, seksjonsvarJsonForLagring, dokumentasjonskrav, pdfGrunnlag)
        seksjonRepository.lagre(
            søknadId,
            ident,
            seksjonId2,
            seksjonsvar2JsonForLagring,
            dokumentasjonskrav2,
            pdfGrunnlag2,
        )
        seksjonRepository.lagre(søknadId, ident, seksjonId3, seksjonsvar3JsonForLagring, pdfGrunnlag = pdfGrunnlag3)

        val dokumentasjonskravForSøknad = seksjonRepository.hentDokumentasjonskrav(søknadId, ident)

        dokumentasjonskravForSøknad shouldContainExactly
            listOf(
                dokumentasjonskrav,
                dokumentasjonskrav2,
            )
    }

    @Test
    fun `hentDokumentasjonskrav returnerer tom liste hvis søknaden tilhører en annen bruker enn den som gjør kallet`() {
        val søknadId = randomUUID()
        søknadRepository.opprett(Søknad(søknadId, ident))
        seksjonRepository.lagre(søknadId, ident, seksjonId, seksjonsvarJsonForLagring, dokumentasjonskrav, pdfGrunnlag)

        seksjonRepository.hentDokumentasjonskrav(søknadId, "en-annen-ident") shouldBe emptyList()
    }

    @Test
    fun `hentDokumentasjonskrav returnerer tom liste hvis søknaden ikke eksisterer`() {
        val søknadId = randomUUID()
        søknadRepository.opprett(Søknad(søknadId, ident))
        seksjonRepository.lagre(søknadId, ident, seksjonId, seksjonsvarJsonForLagring, dokumentasjonskrav, pdfGrunnlag)

        seksjonRepository.hentDokumentasjonskrav(randomUUID(), ident) shouldBe emptyList()
    }

    @Test
    fun `hentPdfGrunnlag returnerer forventede seksjoner hvis søknaden tilhører bruker som gjør kallet`() {
        val søknadId = randomUUID()
        søknadRepository.opprett(Søknad(søknadId, ident))
        seksjonRepository.lagre(søknadId, ident, seksjonId, seksjonsvarJsonForLagring, dokumentasjonskrav, pdfGrunnlag)
        seksjonRepository.lagre(
            søknadId,
            ident,
            seksjonId2,
            seksjonsvar2JsonForLagring,
            dokumentasjonskrav2,
            pdfGrunnlag2,
        )

        val dokumentasjonskravForSøknad = seksjonRepository.hentPdfGrunnlag(søknadId, ident)

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
        seksjonRepository.lagre(søknadId, ident, seksjonId, seksjonsvarJsonForLagring, dokumentasjonskrav, pdfGrunnlag)

        seksjonRepository.hentPdfGrunnlag(søknadId, "en-annen-ident") shouldBe emptyList()
    }

    @Test
    fun `hentPdfGrunnlag returnerer tom liste hvis søknaden ikke eksisterer`() {
        val søknadId = randomUUID()
        søknadRepository.opprett(Søknad(søknadId, ident))
        seksjonRepository.lagre(søknadId, ident, seksjonId, seksjonsvarJsonForLagring, dokumentasjonskrav, pdfGrunnlag)

        seksjonRepository.hentPdfGrunnlag(randomUUID(), ident) shouldBe emptyList()
    }

    @Test
    fun `slettAlleSeksjoner sletter forventede seksjoner hvis søknaden tilhører bruker som gjør kallet`() {
        val søknadId1 = randomUUID()
        val søknadId2 = randomUUID()
        søknadRepository.opprett(Søknad(søknadId1, ident))
        søknadRepository.opprett(Søknad(søknadId2, ident))
        seksjonRepository.lagre(søknadId1, ident, seksjonId, seksjonsvarJsonForLagring, dokumentasjonskrav, pdfGrunnlag)
        seksjonRepository.lagre(
            søknadId1,
            ident,
            "seksjonId2",
            seksjonsvarJsonForLagring,
            dokumentasjonskrav,
            pdfGrunnlag,
        )
        seksjonRepository.lagre(søknadId2, ident, seksjonId, seksjonsvarJsonForLagring, dokumentasjonskrav, pdfGrunnlag)
        seksjonRepository.lagre(
            søknadId2,
            ident,
            "seksjonId2",
            seksjonsvarJsonForLagring,
            dokumentasjonskrav,
            pdfGrunnlag,
        )

        seksjonRepository.slettAlleSeksjoner(søknadId1, ident)

        seksjonRepository.hentSeksjoner(søknadId1, ident) shouldBe emptyList()
        seksjonRepository.hentSeksjoner(søknadId2, ident).size shouldBe 2
    }

    @Test
    fun `slettAlleSeksjoner kaster exception hvis søknaden ikke eksisterer`() {
        val søknadId = randomUUID()

        val exception =
            shouldThrow<IllegalArgumentException> {
                seksjonRepository.slettAlleSeksjoner(søknadId, "en-annen-ident")
            }

        exception.message shouldBe "Fant ikke søknad med ID $søknadId"
    }

    @Test
    fun `slettAlleSeksjoner kaster exception hvis søknaden ikke tilhører bruker som gjør kallet`() {
        val søknadId = randomUUID()
        søknadRepository.opprett(Søknad(søknadId, ident))

        val exception =
            shouldThrow<IllegalArgumentException> {
                seksjonRepository.slettAlleSeksjoner(søknadId, "en-annen-ident")
            }

        exception.message shouldBe "Søknad $søknadId tilhører ikke identen som gjør kallet"
    }
}
