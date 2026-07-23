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
import java.time.LocalDateTime
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
    fun `vi kan sende ut melding om ny søknad på rapiden`() {
        val søknadId = randomUUID()

        søknadService.publiserMeldingOmSøknadInnsendt(søknadId, ident)

        with(testRapid.inspektør) {
            size shouldBe 1
            field(0, "@event_name").asString() shouldBe "søknad_innsendt"
            field(0, "søknadId").asString() shouldBe søknadId.toString()
            field(0, "ident").asString() shouldBe ident
        }
    }

    @Test
    @Suppress("ktlint:standard:max-line-length")
    fun `slettSøknadOgInkrementerMetrikk gjør kall til repository med forventet søknadId og sender forventet melding`() {
        val søknadId = randomUUID()

        val søknad = Søknad(ident = ident, søknadId = søknadId)

        every {
            søknadRepository.hent(søknad.søknadId)
        } returns søknad

        søknadService.slettSøknadOgInkrementerMetrikk(søknadId, ident, "ny")

        verify { søknadRepository.slett(søknadId, ident) }
        with(testRapid.inspektør) {
            size shouldBe 1

            field(0, "@event_name").asString() shouldBe "søknad_endret_tilstand"
            field(0, "gjeldendeTilstand").asString() shouldBe "Slettet"
            field(0, "søknad_uuid").asString() shouldBe søknadId.toString()
            field(0, "ident").asString() shouldBe ident
            testRapid.inspektør.message(0).has("søknadsdata") shouldBe false
        }
    }

    @Test
    @Suppress("ktlint:standard:max-line-length")
    fun `slettSøknadOgInkrementerMetrikk returner uten å gjøre noe om søknaden ikke eksisterer`() {
        val søknadId = randomUUID()

        every {
            søknadRepository.hent(søknadId)
        } returns null

        søknadService.slettSøknadOgInkrementerMetrikk(søknadId, ident, "ny")

        verify(exactly = 0) { søknadRepository.slett(søknadId, ident) }
        with(testRapid.inspektør) {
            size shouldBe 0
        }
    }

    @Test
    @Suppress("ktlint:standard:max-line-length")
    fun `slettSøknadOgInkrementerMetrikk returnerer uten å gjøre noe for alle tilstander som ikke er PÅBEGYNT`() {
        val tilstanderSomIkkeSkalSlettes =
            listOf(
                Tilstand.INNSENDT,
                Tilstand.JOURNALFØRT,
                Tilstand.SLETTET_AV_SYSTEM,
            )

        tilstanderSomIkkeSkalSlettes.forEach { tilstand ->
            testRapid.reset() // Nullstill testrapid mellom iterasjoner
            val søknadId = randomUUID()
            val søknad = Søknad(ident = ident, søknadId = søknadId, tilstand = tilstand)

            every {
                søknadRepository.hent(søknadId)
            } returns søknad

            søknadService.slettSøknadOgInkrementerMetrikk(søknadId, ident, "ny")

            verify(exactly = 0) { søknadRepository.slett(søknadId, ident) }
            testRapid.inspektør.size shouldBe 0
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
        testRapid.inspektør.message(0)["@event_name"].asString() shouldBe "søknad_klar_til_journalføring"
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
            size shouldBe 4
            field(0, "@event_name").asString() shouldBe "søknad_slettet"
            field(0, "søknad_uuid").asString() shouldBe søknadId1.toString()
            field(0, "ident").asString() shouldBe ident1

            field(1, "@event_name").asString() shouldBe "søknad_endret_tilstand"
            field(1, "søknad_uuid").asString() shouldBe søknadId1.toString()
            field(1, "ident").asString() shouldBe ident1

            field(2, "@event_name").asString() shouldBe "søknad_slettet"
            field(2, "søknad_uuid").asString() shouldBe søknadId2.toString()
            field(2, "ident").asString() shouldBe ident2

            field(3, "@event_name").asString() shouldBe "søknad_endret_tilstand"
            field(3, "søknad_uuid").asString() shouldBe søknadId2.toString()
            field(3, "ident").asString() shouldBe ident2
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

    @Test
    fun `finnSendSenereDokumentasjonskraveneForEnSøknad returnerer tom list hvis ingen krav er send senere`() {
        val søknadId = randomUUID()

        every { seksjonRepository.hentDokumentasjonskrav(søknadId, ident) } returns
            dokumentasjonskravUtenSendSenere

        val dokumenter = søknadService.finnSendSenereDokumentasjonskraveneForEnSøknad(søknadId, ident)
        dokumenter.size shouldBe 0
    }

    @Test
    fun `finnSendSenereDokumentasjonskraveneForEnSøknad returnerer bare krav som skal sendes senere`() {
        val søknadId = randomUUID()

        every { seksjonRepository.hentDokumentasjonskrav(søknadId, ident) } returns
            dokumentasjonskravMedBlantAnnetSendSenereSvar

        val dokumenter = søknadService.finnSendSenereDokumentasjonskraveneForEnSøknad(søknadId, ident)

        dokumenter.size shouldBe 2

        val payloader = dokumenter.map { objectMapper.readTree(it) }

        payloader.map { it.findValue("skjemakode").asString() } shouldBe listOf("T8", "T6")
        payloader.map { it.findValue("svar").asString() } shouldBe
            listOf(
                "dokumentkravSvarSenderSenere",
                "dokumentkravSvarSenderSenere",
            )
    }

    @Test
    fun `hentSistOppdatertTidspunkt returnerer forventet tidspunkt`() {
        val søknadId = randomUUID()
        val forventetTidspunkt = LocalDateTime.now()

        every { søknadRepository.hent(any()) } returns Søknad(ident = ident, oppdatertTidspunkt = forventetTidspunkt)

        val faktiskTidspunkt = søknadService.hentSistOppdatertTidspunkt(søknadId)

        faktiskTidspunkt shouldBe forventetTidspunkt
        verify(exactly = 1) { søknadRepository.hent(søknadId) }
    }

    @Test
    fun `hentSistOppdatertTidspunkt returnerer null når søknad ikke finnes`() {
        val søknadId = randomUUID()

        every { søknadRepository.hent(any()) } returns null

        val faktiskTidspunkt = søknadService.hentSistOppdatertTidspunkt(søknadId)

        faktiskTidspunkt shouldBe null
        verify(exactly = 1) { søknadRepository.hent(søknadId) }
    }

    @Test
    fun `hentSistOppdatertTidspunkt returnerer null når oppdatertTidspunkt er null`() {
        val søknadId = randomUUID()

        every { søknadRepository.hent(any()) } returns Søknad(ident = ident, oppdatertTidspunkt = null)

        val faktiskTidspunkt = søknadService.hentSistOppdatertTidspunkt(søknadId)

        faktiskTidspunkt shouldBe null
        verify(exactly = 1) { søknadRepository.hent(søknadId) }
    }

    @Test
    fun `Henter søknader for person og setter riktig skjemakode for permittert og gjenopptak søknad`() {
        every { seksjonRepository.hentSeksjonsvar(any(), any(), "arbeidsforhold") } returns erPermittertJson
        every { seksjonRepository.hentSeksjonsvar(any(), any(), "din-situasjon") } returns erGjenopptakJson
        val søknadForIdent =
            SøknadForIdent(
                søknadId = randomUUID(),
                innsendtTimestamp = LocalDateTime.now(),
                status = "INNSENDT",
            )

        every { søknadRepository.hentSoknaderForIdent(ident) } returns listOf(søknadForIdent)

        val søknader =
            søknadService
                .hentSøknaderForIdent(ident)
                .firstOrNull { it.søknadId == søknadForIdent.søknadId }

        søknader shouldNotBe null
        søknader!!.tittel shouldBe "Søknad om gjenopptak av dagpenger ved permittering"
    }

    @Test
    fun `Henter søknader for person og setter riktig skjemakode for permittert men ikke gjenopptak søknad`() {
        every { seksjonRepository.hentSeksjonsvar(any(), any(), "arbeidsforhold") } returns erPermittertJson
        every { seksjonRepository.hentSeksjonsvar(any(), any(), "din-situasjon") } returns erIkkeGjenopptakJson
        val søknadForIdent =
            SøknadForIdent(
                søknadId = randomUUID(),
                innsendtTimestamp = LocalDateTime.now(),
                status = "INNSENDT",
            )

        every { søknadRepository.hentSoknaderForIdent(ident) } returns listOf(søknadForIdent)

        val søknader =
            søknadService
                .hentSøknaderForIdent(ident)
                .firstOrNull { it.søknadId == søknadForIdent.søknadId }

        søknader shouldNotBe null
        søknader!!.tittel shouldBe "Søknad om dagpenger ved permittering"
    }

    @Test
    fun `Henter søknader for person og setter riktig skjemakode for ikke permittert men gjenopptak søknad`() {
        every { seksjonRepository.hentSeksjonsvar(any(), any(), "arbeidsforhold") } returns erIkkePermittertJson
        every { seksjonRepository.hentSeksjonsvar(any(), any(), "din-situasjon") } returns erGjenopptakJson
        val søknadForIdent =
            SøknadForIdent(
                søknadId = randomUUID(),
                innsendtTimestamp = LocalDateTime.now(),
                status = "INNSENDT",
            )

        every { søknadRepository.hentSoknaderForIdent(ident) } returns listOf(søknadForIdent)

        val søknader =
            søknadService
                .hentSøknaderForIdent(ident)
                .firstOrNull { it.søknadId == søknadForIdent.søknadId }

        søknader shouldNotBe null
        søknader!!.tittel shouldBe "Søknad om gjenopptak av dagpenger"
    }

    @Test
    fun `Henter søknader for person og setter riktig skjemakode for ikke permittert og ikk gjenopptak søknad`() {
        every { seksjonRepository.hentSeksjonsvar(any(), any(), "arbeidsforhold") } returns erIkkePermittertJson
        every { seksjonRepository.hentSeksjonsvar(any(), any(), "din-situasjon") } returns erIkkeGjenopptakJson
        val søknadForIdent =
            SøknadForIdent(
                søknadId = randomUUID(),
                innsendtTimestamp = LocalDateTime.now(),
                status = "INNSENDT",
            )

        every { søknadRepository.hentSoknaderForIdent(ident) } returns listOf(søknadForIdent)

        val søknader =
            søknadService
                .hentSøknaderForIdent(ident)
                .firstOrNull { it.søknadId == søknadForIdent.søknadId }

        søknader shouldNotBe null
        søknader!!.tittel shouldBe "Søknad om dagpenger (ikke permittert)"
    }

    @Test
    fun `Returnerer tom liste hvis det ikke finnes søknad på søkeren`() {
        every { søknadRepository.hentSoknaderForIdent(ident) } returns listOf()

        val søknader =
            søknadService
                .hentSøknaderForIdent(ident)

        søknader shouldNotBe null
        søknader shouldBe emptyList()
    }

    @Test
    fun `Returnerer Søknad om dagpenger (ikke permittert) hvis det ikke finnes data for seksjonene`() {
        every { seksjonRepository.hentSeksjonsvar(any(), any(), "arbeidsforhold") } returns null
        every { seksjonRepository.hentSeksjonsvar(any(), any(), "din-situasjon") } returns null

        val søknadForIdent =
            SøknadForIdent(
                søknadId = randomUUID(),
                innsendtTimestamp = LocalDateTime.now(),
                status = "INNSENDT",
            )

        every { søknadRepository.hentSoknaderForIdent(ident) } returns listOf(søknadForIdent)

        val søknader =
            søknadService
                .hentSøknaderForIdent(ident)
                .firstOrNull { it.søknadId == søknadForIdent.søknadId }

        søknader shouldNotBe null
        søknader!!.tittel shouldBe "Søknad om dagpenger (ikke permittert)"
    }

    @Test
    fun `Returnerer Søknad om dagpenger (ikke permittert) hvis spørsmålene ikke har blitt svart`() {
        every { seksjonRepository.hentSeksjonsvar(any(), any(), "arbeidsforhold") } returns erPermittertIkkeSvartJson
        every { seksjonRepository.hentSeksjonsvar(any(), any(), "din-situasjon") } returns gjenopptakJsonIkkeSvart

        val søknadForIdent =
            SøknadForIdent(
                søknadId = randomUUID(),
                innsendtTimestamp = LocalDateTime.now(),
                status = "INNSENDT",
            )

        every { søknadRepository.hentSoknaderForIdent(ident) } returns listOf(søknadForIdent)

        val søknader =
            søknadService
                .hentSøknaderForIdent(ident)
                .firstOrNull { it.søknadId == søknadForIdent.søknadId }

        søknader shouldNotBe null
        søknader!!.tittel shouldBe "Søknad om dagpenger (ikke permittert)"
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

    private val erPermittertJson =
        """
        {
            "seksjonId": "arbeidsforhold",
            "seksjon": {
            "registrerteArbeidsforhold": [
                {
                    "hvordanHarDetteArbeidsforholdetEndretSeg": "jegErPermittert"
                }
            ]
            },
            "versjon": 1
        }
        """.trimIndent()

    private val erIkkePermittertJson =
        """
        {
            "seksjonId": "arbeidsforhold",
            "seksjon": {
            "registrerteArbeidsforhold": [
                {
                    "hvordanHarDetteArbeidsforholdetEndretSeg": "arbeidsgiverErKonkurs"
                }
            ]
            },
            "versjon": 1
        }
        """.trimIndent()

    private val erPermittertIkkeSvartJson =
        """
        {
            "seksjonId": "arbeidsforhold",
            "seksjon": {
            "registrerteArbeidsforhold": [
                {
                }
            ]
            },
            "versjon": 1
        }
        """.trimIndent()

    private val erGjenopptakJson =
        """
        {
          "seksjonId": "din-situasjon",
          "seksjonsvar": {
            "harDuMottattDagpengerFraNavILøpetAvDeSiste52Ukene": "ja"
          },
          "versjon": 1
        }
        """.trimIndent()

    private val erIkkeGjenopptakJson =
        """
        {
          "seksjonId": "din-situasjon",
          "seksjonsvar": {
            "harDuMottattDagpengerFraNavILøpetAvDeSiste52Ukene": "nei"
          },
          "versjon": 1
        }
        """.trimIndent()

    private val gjenopptakJsonIkkeSvart =
        """
        {
          "seksjonId": "din-situasjon",
          "seksjonsvar": {
          },
          "versjon": 1
        }
        """.trimIndent()

    private val dokumentasjonskravMedBlantAnnetSendSenereSvar =
        listOf(
            """
            [
                {
                  "id": "b8106828-cee3-4634-938a-787fbb828dbb",
                  "seksjonId": "arbeidsforhold",
                  "spørsmålId": "hvordanHarDetteArbeidsforholdetEndretSeg",
                  "skjemakode": "T8",
                  "tittel": "Oppsigelse - Bedrift 1 AS",
                  "type": "ArbeidsforholdArbeidsgiverenMinHarSagtMegOpp",
                  "svar": "dokumentkravSvarSenderSenere",
                  "begrunnelse": "rrr"
                },
                {
                  "id": "ad70b80b-0d24-445d-87d4-7d08dd68d355",
                  "seksjonId": "arbeidsforhold",
                  "spørsmålId": "hvordanHarDetteArbeidsforholdetEndretSeg",
                  "skjemakode": "O2",
                  "tittel": "Arbeidsavtale - Bedrift 2 DA",
                  "type": "ArbeidsforholdArbeidsavtale",
                  "svar": "dokumentkravSvarSenderIkke",
                  "begrunnelse": "bbb?"
                },
                {
                  "id": "5fd32960-4b66-455f-a5e0-df49d8cde9e1",
                  "seksjonId": "arbeidsforhold",
                  "spørsmålId": "hvordanHarDetteArbeidsforholdetEndretSeg",
                  "skjemakode": "T6",
                  "tittel": "Permitteringsvarsel - Bedrift 2 DA",
                  "type": "ArbeidsforholdPermitteringsvarsel",
                  "svar": "dokumentkravSvarSenderIkke",
                  "begrunnelse": "ccc"
                }
                          ]
            """.trimIndent(),
            """
            [
              {
                "id": "93e3d573-6de7-4ba5-96d6-805921eaacf1",
                "seksjonId": "arbeidsforhold",
                "spørsmålId": "hvordanHarDetteArbeidsforholdetEndretSeg",
                "skjemakode": "O2",
                "tittel": "Arbeidsavtale - ddd",
                "type": "ArbeidsforholdArbeidsavtale",
                "svar": "dokumentkravSvarSendNå",
                "filer": [
                  {
                    "id": "24efe9de-5380-493b-97da-766096975d1e",
                    "file": {},
                    "filnavn": "sol.png",
                    "lasterOpp": false,
                    "urn": "urn:vedlegg:3b09cc1b-3fe2-41f4-858f-3557b7a1d9ca/93e3d573-6de7-4ba5-96d6-805921eaacf1/66eefd6e-2f31-424b-98f4-cd44b9f934ee",
                    "filsti": "3b09cc1b-3fe2-41f4-858f-3557b7a1d9ca/93e3d573-6de7-4ba5-96d6-805921eaacf1/66eefd6e-2f31-424b-98f4-cd44b9f934ee",
                    "storrelse": 1879,
                    "tidspunkt": "2026-02-19T08:32:25.550864975+01:00"
                  }
                ],
                "bundle": {
                  "filnavn": "93e3d573-6de7-4ba5-96d6-805921eaacf1",
                  "urn": "urn:vedlegg:3b09cc1b-3fe2-41f4-858f-3557b7a1d9ca/43232246-0bdd-4bd3-9161-34d7217f8496",
                  "filsti": "3b09cc1b-3fe2-41f4-858f-3557b7a1d9ca/43232246-0bdd-4bd3-9161-34d7217f8496",
                  "storrelse": 86085,
                  "tidspunkt": "2026-02-19T08:32:29.331083379+01:00"
                }
              },
              {
                  "id": "5fd32960-4b66-455f-a5e0-df49d8cde9e1",
                  "seksjonId": "arbeidsforhold",
                  "spørsmålId": "hvordanHarDetteArbeidsforholdetEndretSeg",
                  "skjemakode": "T6",
                  "tittel": "Permitteringsvarsel - Bedrift 2 DA",
                  "type": "ArbeidsforholdPermitteringsvarsel",
                  "svar": "dokumentkravSvarSenderSenere",
                  "begrunnelse": "ccc"
                },
                {
                  "id": "5fd32960-4b66-455f-a5e0-df49d8cde9e1",
                  "seksjonId": "arbeidsforhold",
                  "spørsmålId": "hvordanHarDetteArbeidsforholdetEndretSeg",
                  "skjemakode": "T6",
                  "tittel": "Permitteringsvarsel - Bedrift 2 DA",
                  "type": "ArbeidsforholdPermitteringsvarsel",
                  "begrunnelse": "ccc"
                },
              {
                "id": "a67bce29-eb2a-4c1d-8433-69ac9fd63a33",
                "seksjonId": "arbeidsforhold",
                "spørsmålId": "hvordanHarDetteArbeidsforholdetEndretSeg",
                "skjemakode": "T8",
                "tittel": "Oppsigelse - ddd",
                "type": "ArbeidsforholdArbeidsgiverenMinHarSagtMegOpp",
                "svar": "dokumentkravEttersendt",
                "filer": [
                  {
                    "id": "e9ff1647-aea6-4be3-9414-5a66c4ea29ef",
                    "file": {},
                    "filnavn": "oppsigelse.png",
                    "lasterOpp": false,
                    "urn": "urn:vedlegg:3b09cc1b-3fe2-41f4-858f-3557b7a1d9ca/a67bce29-eb2a-4c1d-8433-69ac9fd63a33/8bebe9b5-503f-48a1-aaf9-80ea1c8f4685",
                    "filsti": "3b09cc1b-3fe2-41f4-858f-3557b7a1d9ca/a67bce29-eb2a-4c1d-8433-69ac9fd63a33/8bebe9b5-503f-48a1-aaf9-80ea1c8f4685",
                    "storrelse": 4626,
                    "tidspunkt": "2026-02-19T08:38:52.532296475+01:00"
                  }
                ],
                "bundle": {
                  "filnavn": "a67bce29-eb2a-4c1d-8433-69ac9fd63a33",
                  "urn": "urn:vedlegg:3b09cc1b-3fe2-41f4-858f-3557b7a1d9ca/7820f4b0-cc66-4ac1-a6ae-eef1255102ea",
                  "filsti": "3b09cc1b-3fe2-41f4-858f-3557b7a1d9ca/7820f4b0-cc66-4ac1-a6ae-eef1255102ea",
                  "storrelse": 150602,
                  "tidspunkt": "2026-02-19T08:38:55.90602091+01:00"
                }
              }
            ]
            """.trimIndent(),
        )

    private val dokumentasjonskravUtenSendSenere =
        listOf(
            """
            [
                {
                  "id": "ad70b80b-0d24-445d-87d4-7d08dd68d355",
                  "seksjonId": "arbeidsforhold",
                  "spørsmålId": "hvordanHarDetteArbeidsforholdetEndretSeg",
                  "skjemakode": "O2",
                  "tittel": "Arbeidsavtale - Bedrift 2 DA",
                  "type": "ArbeidsforholdArbeidsavtale",
                  "svar": "dokumentkravSvarSenderIkke",
                  "begrunnelse": "bbb?"
                },
                {
                  "id": "5fd32960-4b66-455f-a5e0-df49d8cde9e1",
                  "seksjonId": "arbeidsforhold",
                  "spørsmålId": "hvordanHarDetteArbeidsforholdetEndretSeg",
                  "skjemakode": "T6",
                  "tittel": "Permitteringsvarsel - Bedrift 2 DA",
                  "type": "ArbeidsforholdPermitteringsvarsel",
                  "svar": "dokumentkravSvarSenderIkke",
                  "begrunnelse": "ccc"
                }
                          ]
            """.trimIndent(),
            """
            [
              {
                "id": "93e3d573-6de7-4ba5-96d6-805921eaacf1",
                "seksjonId": "arbeidsforhold",
                "spørsmålId": "hvordanHarDetteArbeidsforholdetEndretSeg",
                "skjemakode": "O2",
                "tittel": "Arbeidsavtale - ddd",
                "type": "ArbeidsforholdArbeidsavtale",
                "svar": "dokumentkravSvarSendNå",
                "filer": [
                  {
                    "id": "24efe9de-5380-493b-97da-766096975d1e",
                    "file": {},
                    "filnavn": "sol.png",
                    "lasterOpp": false,
                    "urn": "urn:vedlegg:3b09cc1b-3fe2-41f4-858f-3557b7a1d9ca/93e3d573-6de7-4ba5-96d6-805921eaacf1/66eefd6e-2f31-424b-98f4-cd44b9f934ee",
                    "filsti": "3b09cc1b-3fe2-41f4-858f-3557b7a1d9ca/93e3d573-6de7-4ba5-96d6-805921eaacf1/66eefd6e-2f31-424b-98f4-cd44b9f934ee",
                    "storrelse": 1879,
                    "tidspunkt": "2026-02-19T08:32:25.550864975+01:00"
                  }
                ],
                "bundle": {
                  "filnavn": "93e3d573-6de7-4ba5-96d6-805921eaacf1",
                  "urn": "urn:vedlegg:3b09cc1b-3fe2-41f4-858f-3557b7a1d9ca/43232246-0bdd-4bd3-9161-34d7217f8496",
                  "filsti": "3b09cc1b-3fe2-41f4-858f-3557b7a1d9ca/43232246-0bdd-4bd3-9161-34d7217f8496",
                  "storrelse": 86085,
                  "tidspunkt": "2026-02-19T08:32:29.331083379+01:00"
                }
              },
              {
                "id": "a67bce29-eb2a-4c1d-8433-69ac9fd63a33",
                "seksjonId": "arbeidsforhold",
                "spørsmålId": "hvordanHarDetteArbeidsforholdetEndretSeg",
                "skjemakode": "T8",
                "tittel": "Oppsigelse - ddd",
                "type": "ArbeidsforholdArbeidsgiverenMinHarSagtMegOpp",
                "svar": "dokumentkravEttersendt",
                "filer": [
                  {
                    "id": "e9ff1647-aea6-4be3-9414-5a66c4ea29ef",
                    "file": {},
                    "filnavn": "oppsigelse.png",
                    "lasterOpp": false,
                    "urn": "urn:vedlegg:3b09cc1b-3fe2-41f4-858f-3557b7a1d9ca/a67bce29-eb2a-4c1d-8433-69ac9fd63a33/8bebe9b5-503f-48a1-aaf9-80ea1c8f4685",
                    "filsti": "3b09cc1b-3fe2-41f4-858f-3557b7a1d9ca/a67bce29-eb2a-4c1d-8433-69ac9fd63a33/8bebe9b5-503f-48a1-aaf9-80ea1c8f4685",
                    "storrelse": 4626,
                    "tidspunkt": "2026-02-19T08:38:52.532296475+01:00"
                  }
                ],
                "bundle": {
                  "filnavn": "a67bce29-eb2a-4c1d-8433-69ac9fd63a33",
                  "urn": "urn:vedlegg:3b09cc1b-3fe2-41f4-858f-3557b7a1d9ca/7820f4b0-cc66-4ac1-a6ae-eef1255102ea",
                  "filsti": "3b09cc1b-3fe2-41f4-858f-3557b7a1d9ca/7820f4b0-cc66-4ac1-a6ae-eef1255102ea",
                  "storrelse": 150602,
                  "tidspunkt": "2026-02-19T08:38:55.90602091+01:00"
                }
              }
            ]
            """.trimIndent(),
        )
}
