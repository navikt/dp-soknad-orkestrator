package no.nav.dagpenger.soknad.orkestrator.søknad

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.inspectors.shouldForAtMostOne
import io.kotest.matchers.collections.shouldContainNoNulls
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import no.nav.dagpenger.soknad.orkestrator.opplysning.seksjoner.Seksjon
import no.nav.dagpenger.soknad.orkestrator.opplysning.seksjoner.Seksjonsnavn
import no.nav.dagpenger.soknad.orkestrator.opplysning.seksjoner.getSeksjon
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadPersonaliaRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.util.UUID.randomUUID
import kotlin.test.Test

class SøknadServiceTest {
    private val testRapid = TestRapid()
    private val søknadRepository = mockk<SøknadRepository>(relaxed = true)
    private val søknadPersonaliaRepository = mockk<SøknadPersonaliaRepository>(relaxed = true)
    private val seksjonRepository = mockk<SeksjonRepository>(relaxed = true)
    private val seksjon = mockk<Seksjon>(relaxed = true)
    private var søknadService =
        SøknadService(
            søknadRepository = søknadRepository,
            søknadPersonaliaRepository = søknadPersonaliaRepository,
            seksjonRepository = seksjonRepository,
        ).also { it.setRapidsConnection(testRapid) }
    private val ident = "12345678901"
    private val seksjonPath = "no.nav.dagpenger.soknad.orkestrator.opplysning.seksjoner.SeksjonKt"

    @BeforeEach
    fun setup() {
        mockkStatic(seksjonPath)
        every { getSeksjon(any()) } returns seksjon
        every { seksjon.navn } returns Seksjonsnavn.BOSTEDSLAND
    }

    @AfterEach
    fun reset() {
        clearMocks(søknadRepository, seksjon)
        unmockkStatic(seksjonPath)
    }

    @Test
    fun `SøknadFinnes returnerer true når søknad finnes i databasen`() {
        val søknad = Søknad(ident = ident)

        every {
            søknadRepository.hent(søknad.søknadId)
        } returns søknad

        søknadService.søknadFinnes(søknad.søknadId) shouldBe true
    }

    @Test
    fun `SøknadFinnes returnerer false når søknaden ikke finnes i databasen`() {
        every {
            søknadRepository.hent(any())
        } returns null

        søknadService.søknadFinnes(randomUUID()) shouldBe false
    }

    @Test
    fun `Kan opprette komplett søknadData med quiz-seksjoner`() {
        val ident = "12345678901"
        val søknadId = randomUUID()
        val seksjoner = objectMapper.readTree(quizSeksjoner)

        val søknadData =
            søknadService.opprettOgLagreKomplettSøknaddata(ident = ident, søknadId = søknadId, seksjoner = seksjoner)

        verify(exactly = 1) { søknadRepository.lagreKomplettSøknadData(søknadId, any()) }
        søknadData["ident"].asText() shouldBe ident
        søknadData["søknadId"].asText() shouldBe søknadId.toString()
        søknadData["seksjoner"] shouldBe seksjoner
    }

    @Test
    fun `vi kan sende ut melding om ny søknad på rapiden`() {
        val søknadId = randomUUID()

        søknadService.publiserMeldingOmSøknadInnsendt(søknadId, ident)

        with(testRapid.inspektør) {
            size shouldBe 1
            field(0, "@event_name").asText() shouldBe "søknad_innsendt"
            field(0, "søknadId").asText() shouldBe søknadId.toString()
            field(0, "ident").asText() shouldBe ident
        }
    }

    @Test
    @Suppress("ktlint:standard:max-line-length")
    fun `slettSøknadInkrementerMetrikkOgSendMeldingOmSletting gjør kall til repository med forventet søknadId og sender forventet melding`() {
        val søknadId = randomUUID()

        søknadService.slettSøknadInkrementerMetrikkOgSendMeldingOmSletting(søknadId, ident)

        verify { søknadRepository.slett(søknadId, ident) }
        with(testRapid.inspektør) {
            size shouldBe 1
            field(0, "@event_name").asText() shouldBe "søknad_slettet"
            field(0, "søknad_uuid").asText() shouldBe søknadId.toString()
            field(0, "ident").asText() shouldBe ident
        }
    }

    @Test
    fun `opprett returnerer UUID fra repository`() {
        val søknadId = randomUUID()
        coEvery { søknadRepository.opprett(any()) } returns søknadId

        søknadService.opprett(ident) shouldBe søknadId
    }

    @Test
    fun `sendInn publiserer forventet melding på rapidsConnection`() {
        søknadService.sendInn(randomUUID(), ident)

        testRapid.inspektør.size shouldBe 1
        testRapid.inspektør.message(0)["@event_name"].asText() shouldBe "søknad_klar_til_journalføring"
    }

    @Test
    fun `slettSøknaderSomErPåbegyntOgIkkeOppdatertPå7Dager sletter alle søknader som skal slettes og sender forventede meldinger`() {
        val søknadId1 = randomUUID()
        val søknadId2 = randomUUID()
        val ident1 = "ident1"
        val ident2 = "ident2"
        every { søknadRepository.hentAlleSøknaderSomErPåbegyntOgIkkeOppdatertPå7Dager() } returns
            listOf(
                Søknad(søknadId1, ident1),
                Søknad(søknadId2, ident2),
            )

        søknadService.slettSøknaderSomErPåbegyntOgIkkeOppdatertPå7Dager()

        verify { seksjonRepository.slettAlleSeksjoner(søknadId1, ident1) }
        verify { søknadRepository.slettSøknadSomSystem(søknadId1, ident1, any()) }
        verify { seksjonRepository.slettAlleSeksjoner(søknadId2, ident2) }
        verify { søknadRepository.slettSøknadSomSystem(søknadId2, ident2, any()) }
        with(testRapid.inspektør) {
            size shouldBe 2
            field(0, "@event_name").asText() shouldBe "søknad_slettet"
            field(0, "søknad_uuid").asText() shouldBe søknadId1.toString()
            field(0, "ident").asText() shouldBe ident1
            field(1, "@event_name").asText() shouldBe "søknad_slettet"
            field(1, "søknad_uuid").asText() shouldBe søknadId2.toString()
            field(1, "ident").asText() shouldBe ident2
        }
    }

    @Test
    @Suppress("ktlint:standard:max-line-length")
    fun `slettSøknaderSomErPåbegyntOgIkkeOppdatertPå7Dager sletter ingen søknader hvis det ikke eksisterer noen søkander som skal slettes`() {
        every { søknadRepository.hentAlleSøknaderSomErPåbegyntOgIkkeOppdatertPå7Dager() } returns emptyList()

        søknadService.slettSøknaderSomErPåbegyntOgIkkeOppdatertPå7Dager()

        verify(exactly = 0) { seksjonRepository.slettAlleSeksjoner(randomUUID(), ident) }
        verify(exactly = 0) { søknadRepository.slettSøknadSomSystem(randomUUID(), ident, any()) }
    }

    @Test
    fun `opprettDokumenterFraDokumentasjonskrav returnerer forventede dokumenter`() {
        every { seksjonRepository.hentDokumentasjonskrav(any(), any()) } returns
            listOf(
                this::class.java
                    .getResource("/testdata/dokumentasjonskrav-barnetillegg.json")!!
                    .readText(Charsets.UTF_8),
                this::class.java
                    .getResource("/testdata/dokumentasjonskrav-arbeidsforhold.json")!!
                    .readText(Charsets.UTF_8),
                this::class.java
                    .getResource("/testdata/dokumentasjonskrav-annen-pengestøtte.json")!!
                    .readText(Charsets.UTF_8),
                this::class.java
                    .getResource("/testdata/dokumentasjonskrav-verneplikt.json")!!
                    .readText(Charsets.UTF_8),
            )

        val dokumenter = søknadService.opprettDokumenterFraDokumentasjonskrav(randomUUID(), ident)

        dokumenter.size shouldBe 3
        dokumenter.shouldContainNoNulls()
        dokumenter.shouldForAtMostOne { dokument ->
            dokument shouldBe "02"
            dokument.varianter.size shouldBe 1
            dokument.varianter[0].uuid shouldNotBe null
            dokument.varianter[0].filnavn shouldBe "ebf48dd8-e3df-4ab0-a015-d9109e2dc000"
            dokument.varianter[0].urn shouldBe "urn:vedlegg:ad96be1e-a3d0-46c7-a869-1d7ddf01933c/87d8abbf-cf03-46aa-9659-4b74eaa3c8d0"
            dokument.varianter[0].variant shouldBe "ARKIV"
            dokument.varianter[0].type shouldBe "PDF"
        }
        dokumenter.shouldForAtMostOne { dokument ->
            dokument shouldBe "T6"
            dokument.varianter.size shouldBe 1
            dokument.varianter[0].uuid shouldNotBe null
            dokument.varianter[0].filnavn shouldBe "ebf48dd8-e3df-4ab0-a015-d9109e2dc001"
            dokument.varianter[0].urn shouldBe "urn:vedlegg:ad96be1e-a3d0-46c7-a869-1d7ddf01933c/87d8abbf-cf03-46aa-9659-4b74eaa3c8d1"
            dokument.varianter[0].variant shouldBe "ARKIV"
            dokument.varianter[0].type shouldBe "PDF"
        }
        dokumenter.shouldForAtMostOne { dokument ->
            dokument shouldBe "X8"
            dokument.varianter.size shouldBe 1
            dokument.varianter[0].uuid shouldNotBe null
            dokument.varianter[0].filnavn shouldBe "ebf48dd8-e3df-4ab0-a015-d9109e2dc002"
            dokument.varianter[0].urn shouldBe "urn:vedlegg:ad96be1e-a3d0-46c7-a869-1d7ddf01933c/87d8abbf-cf03-46aa-9659-4b74eaa3c8d2"
            dokument.varianter[0].variant shouldBe "ARKIV"
            dokument.varianter[0].type shouldBe "PDF"
        }
    }

    private val quizSeksjoner =
        //language=json
        """
        {
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
            },
            {
              "fakta": [
                {
                  "id": "7001",
                  "svar": "true",
                  "type": "boolean",
                  "beskrivendeId": "faktum.avtjent-militaer-sivilforsvar-tjeneste-siste-12-mnd"
                }
              ],
              "beskrivendeId": "verneplikt"
            }
          ]
        }
        """.trimIndent()
}
