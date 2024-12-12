package no.nav.dagpenger.soknad.orkestrator.søknad

import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import no.nav.dagpenger.soknad.orkestrator.opplysning.LandSvar
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysning
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysningsbehov
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysningstype
import no.nav.dagpenger.soknad.orkestrator.opplysning.PeriodesvarSvar
import no.nav.dagpenger.soknad.orkestrator.opplysning.db.OpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.opplysning.seksjoner.Seksjon
import no.nav.dagpenger.soknad.orkestrator.opplysning.seksjoner.Seksjonsnavn
import no.nav.dagpenger.soknad.orkestrator.opplysning.seksjoner.getSeksjon
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.PeriodeSvar
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.utils.asUUID
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test

class SøknadServiceTest {
    private val testRapid = TestRapid()
    private val søknadRepository = mockk<SøknadRepository>(relaxed = true)
    private val opplysningRepository = mockk<OpplysningRepository>(relaxed = true)
    private val seksjon = mockk<Seksjon>(relaxed = true)
    private var søknadService =
        SøknadService(
            søknadRepository = søknadRepository,
            opplysningRepository = opplysningRepository,
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
        clearMocks(søknadRepository, seksjon, opplysningRepository)
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

        søknadService.søknadFinnes(UUID.randomUUID()) shouldBe false
    }

    @Test
    fun `Kan opprette komplett søknadData med quiz-seksjoner og uten orkestrator-opplysninger`() {
        val ident = "12345678901"
        val søknadId = UUID.randomUUID()
        val seksjoner = objectMapper.readTree(quizSeksjoner)
        every { opplysningRepository.hentAlle(søknadId) } returns emptyList()

        val søknadData =
            søknadService.opprettOgLagreKomplettSøknaddata(ident = ident, søknadId = søknadId, seksjoner = seksjoner)

        verify(exactly = 1) { søknadRepository.lagreKomplettSøknadData(søknadId, any()) }
        søknadData["ident"].asText() shouldBe ident
        søknadData["søknadId"].asText() shouldBe søknadId.toString()
        søknadData["seksjoner"] shouldBe seksjoner
        søknadData["orkestratorSeksjoner"].size() shouldBe 0
    }

    @Test
    fun `Kan opprette komplett søknadData med quiz-seksjoner og orkestrator-seksjon`() {
        val ident = "12345678901"
        val søknadId = UUID.randomUUID()
        val opplysningId1 = UUID.randomUUID()
        val opplysningId2 = UUID.randomUUID()
        val today = LocalDate.now()
        val seksjoner = objectMapper.readTree(quizSeksjoner)
        every { opplysningRepository.hentAlle(søknadId) } returns
            listOf(
                Opplysning(
                    opplysningId = opplysningId1,
                    seksjonsnavn = seksjon.navn,
                    opplysningsbehovId = 1,
                    type = Opplysningstype.PERIODE,
                    svar =
                        PeriodesvarSvar(
                            opplysningId = opplysningId1,
                            verdi =
                                PeriodeSvar(
                                    fom = today,
                                    tom = today,
                                ),
                        ),
                ),
                Opplysning(
                    opplysningId = opplysningId2,
                    seksjonsnavn = seksjon.navn,
                    opplysningsbehovId = 2,
                    type = Opplysningstype.LAND,
                    svar = LandSvar(opplysningId = opplysningId2, verdi = "NOR"),
                ),
            )

        every { seksjon.getOpplysningsbehov(1) } returns
            Opplysningsbehov(
                id = 1,
                tekstnøkkel = "tekstnøkkel.periode",
                type = Opplysningstype.PERIODE,
            )

        every { seksjon.getOpplysningsbehov(2) } returns
            Opplysningsbehov(
                id = 2,
                tekstnøkkel = "tekstnøkkel.land",
                type = Opplysningstype.LAND,
                gyldigeSvar = listOf("NOR", "SWE", "FIN"),
            )

        val søknadData =
            søknadService.opprettOgLagreKomplettSøknaddata(ident = ident, søknadId = søknadId, seksjoner = seksjoner)

        verify(exactly = 1) { søknadRepository.lagreKomplettSøknadData(søknadId, any()) }
        søknadData["ident"].asText() shouldBe ident
        søknadData["søknadId"].asText() shouldBe søknadId.toString()
        søknadData["seksjoner"] shouldBe seksjoner
        søknadData["orkestratorSeksjoner"].size() shouldBe 1
        søknadData["orkestratorSeksjoner"][0]["seksjonsnavn"].asText() shouldBe seksjon.navn.name
        søknadData["orkestratorSeksjoner"][0]["opplysninger"].size() shouldBe 2
        søknadData["orkestratorSeksjoner"][0]["opplysninger"][0].also {
            it["opplysningId"].asUUID() shouldBe opplysningId1
            it["tekstnøkkel"].asText() shouldBe "tekstnøkkel.periode"
            it["type"].asText() shouldBe Opplysningstype.PERIODE.name
            it["svar"]["fom"].asLocalDate() shouldBe today
            it["svar"]["tom"].asLocalDate() shouldBe today
        }
        søknadData["orkestratorSeksjoner"][0]["opplysninger"][1].also {
            it["opplysningId"].asUUID() shouldBe opplysningId2
            it["tekstnøkkel"].asText() shouldBe "tekstnøkkel.land"
            it["type"].asText() shouldBe Opplysningstype.LAND.name
            it["svar"].asText() shouldBe "NOR"
            it["gyldigeSvar"].size() shouldBe 3
        }
    }

    @Test
    fun `vi kan sende ut melding om ny søknad på rapiden`() {
        val søknadId = UUID.randomUUID()

        søknadService.publiserMeldingOmSøknadInnsendt(søknadId, ident)

        with(testRapid.inspektør) {
            size shouldBe 1
            field(0, "@event_name").asText() shouldBe "søknad_innsendt"
            field(0, "søknadId").asText() shouldBe søknadId.toString()
            field(0, "ident").asText() shouldBe ident
        }
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
