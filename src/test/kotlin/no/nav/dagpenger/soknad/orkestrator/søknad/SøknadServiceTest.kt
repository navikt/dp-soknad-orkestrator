package no.nav.dagpenger.soknad.orkestrator.søknad

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
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
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.util.UUID.randomUUID
import kotlin.test.Test

class SøknadServiceTest {
    private val testRapid = TestRapid()
    private val søknadRepository = mockk<SøknadRepository>(relaxed = true)
    private val seksjon = mockk<Seksjon>(relaxed = true)
    private var søknadService =
        SøknadService(
            søknadRepository = søknadRepository,
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
    fun `slett gjør kall til repository med forventet søknadId`() {
        val søknadId = randomUUID()

        søknadService.slett(søknadId, "ident")

        verify { søknadRepository.slett(søknadId, "ident") }
    }

    @Test
    fun `opprett returnerer UUID fra repository`() {
        val søknadId = randomUUID()
        coEvery { søknadRepository.lagre(any()) } returns søknadId

        søknadService.opprett("ident") shouldBe søknadId
    }

    @Test
    fun `sendInn publiserer forventet melding på rapidsConnection`() {
        søknadService.sendInn(randomUUID(), "ident")

        testRapid.inspektør.size shouldBe 1
        testRapid.inspektør.message(0)["@event_name"].asText() shouldBe "søknad_klar_til_journalføring"
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
