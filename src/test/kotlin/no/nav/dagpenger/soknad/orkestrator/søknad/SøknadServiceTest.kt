package no.nav.dagpenger.soknad.orkestrator.søknad

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import no.nav.dagpenger.soknad.orkestrator.opplysning.BooleanSvar
import no.nav.dagpenger.soknad.orkestrator.opplysning.LandSvar
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysning
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysningsbehov
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysningstype
import no.nav.dagpenger.soknad.orkestrator.opplysning.Svar
import no.nav.dagpenger.soknad.orkestrator.opplysning.db.OpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.opplysning.seksjoner.Seksjon
import no.nav.dagpenger.soknad.orkestrator.opplysning.seksjoner.Seksjonsnavn
import no.nav.dagpenger.soknad.orkestrator.opplysning.seksjoner.getSeksjon
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
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

    @Test
    fun `hentEllerOpprettSøknad oppretter søknad, første opplysning og seksjon`() {
        every {
            søknadRepository.hentPåbegynt(ident)
        } returns null

        justRun {
            søknadRepository.lagre(any())
            opplysningRepository.opprettSeksjon(any(), any())
            opplysningRepository.lagre(any(), any())
        }

        val søknad = søknadService.hentEllerOpprettSøknad(ident)

        verify(exactly = 1) {
            søknadRepository.lagre(søknad)
            opplysningRepository.opprettSeksjon(søknad.søknadId, any())
            opplysningRepository.lagre(any(), any())
        }
    }

    @Test
    fun `hentEllerOpprettSøknad returnerer påbegynt søknad hvis det finnes`() {
        val søknad = Søknad(ident = ident, tilstand = Tilstand.PÅBEGYNT)
        every {
            søknadRepository.hentPåbegynt(ident)
        } returns søknad

        val hentetSøknad = søknadService.hentEllerOpprettSøknad(ident)

        hentetSøknad.søknadId shouldBe søknad.søknadId
        verify(exactly = 0) {
            søknadRepository.lagre(any())
            opplysningRepository.opprettSeksjon(any(), any())
            opplysningRepository.lagre(any(), any())
        }
    }

    @Test
    fun `håndterSvar kaster feil dersom man prøver å besvare en opplysning som ikke finnes`() {
        every {
            opplysningRepository.hent(any())
        } returns null

        val svar = BooleanSvar(opplysningId = UUID.randomUUID(), verdi = true)

        shouldThrow<IllegalArgumentException> {
            søknadService.håndterSvar(UUID.randomUUID(), svar)
        }
    }

    @Test
    fun `håndterSvar validerer og lagrer svar på opplysning`() {
        val svar = BooleanSvar(opplysningId = UUID.randomUUID(), verdi = true)
        søknadService.håndterSvar(UUID.randomUUID(), svar)

        verify(exactly = 1) {
            seksjon.validerSvar(any(), any())
            opplysningRepository.lagreSvar(svar)
        }
    }

    @Test
    fun `håndterSvar nullstiller avhengigheter`() {
        val søknadId = UUID.randomUUID()
        val avhengigOpplysningsbehovId = 1

        every { seksjon.avhengigheter(any()) } returns listOf(avhengigOpplysningsbehovId)

        val svar = LandSvar(opplysningId = UUID.randomUUID(), verdi = "OPP")
        søknadService.håndterSvar(søknadId, svar)

        verify(exactly = 1) {
            seksjon.avhengigheter(any())
            opplysningRepository.slett(søknadId, any(), avhengigOpplysningsbehovId)
        }
    }

    @Test
    fun `håndterSvar nullstiller ingenting hvis det ikke finnes noen avhengigheter`() {
        val søknadId = UUID.randomUUID()
        val avhengigOpplysningsbehovId = 1

        every { seksjon.avhengigheter(any()) } returns emptyList()

        val svar = LandSvar(opplysningId = UUID.randomUUID(), verdi = "OPP")
        søknadService.håndterSvar(søknadId, svar)

        verify(exactly = 0) {
            opplysningRepository.slett(søknadId, any(), avhengigOpplysningsbehovId)
        }
    }

    @Test
    fun `håndterSvar oppretter neste opplysning hvis den ikke finnes allerede`() {
        val søknadId = UUID.randomUUID()
        every { seksjon.nesteOpplysningsbehov(any<Svar<*>>(), any<Int>()) } returns
            Opplysningsbehov(
                id = 2,
                tekstnøkkel = "tekstnøkkel",
                type = Opplysningstype.BOOLEAN,
            )

        val opplysningId = UUID.randomUUID()
        val svar = BooleanSvar(opplysningId = opplysningId, verdi = true)

        every {
            opplysningRepository.hentAlleForSeksjon(søknadId, any())
        } returns emptyList()

        søknadService.håndterSvar(søknadId, svar)

        verify(exactly = 1) {
            opplysningRepository.lagre(søknadId, any())
        }
    }

    @Test
    fun `håndterSvar oppretter ikke neste opplysning hvis den finnes allerede`() {
        val søknadId = UUID.randomUUID()
        val opplysningId = UUID.randomUUID()
        val svar = BooleanSvar(opplysningId = opplysningId, verdi = true)

        every { seksjon.nesteOpplysningsbehov(any<Svar<*>>(), any<Int>()) } returns
            Opplysningsbehov(
                id = 2,
                tekstnøkkel = "tekstnøkkel",
                type = Opplysningstype.BOOLEAN,
            )

        every {
            opplysningRepository.hentAlleForSeksjon(søknadId, any())
        } returns
            listOf(
                Opplysning(
                    opplysningId = UUID.randomUUID(),
                    seksjonsnavn = seksjon.navn,
                    opplysningsbehovId = 2,
                    type = Opplysningstype.BOOLEAN,
                    svar = BooleanSvar(opplysningId = opplysningId, verdi = true),
                ),
            )

        søknadService.håndterSvar(søknadId, svar)

        verify(exactly = 0) {
            opplysningRepository.lagre(any(), any())
        }
    }

    @Test
    fun `nesteSeksjon henter kun besvarte opplysninger som kommer før den ubesvarte opplysningen`() {
        val søknadId = UUID.randomUUID()
        val opplysningId1 = UUID.randomUUID()
        val opplysningId3 = UUID.randomUUID()

        every {
            opplysningRepository.hentAlle(søknadId)
        } returns
            listOf(
                Opplysning(
                    opplysningId = opplysningId1,
                    seksjonsnavn = seksjon.navn,
                    opplysningsbehovId = 1,
                    type = Opplysningstype.BOOLEAN,
                    svar = BooleanSvar(opplysningId = opplysningId1, verdi = true),
                ),
                Opplysning(
                    opplysningId = UUID.randomUUID(),
                    seksjonsnavn = seksjon.navn,
                    opplysningsbehovId = 2,
                    type = Opplysningstype.BOOLEAN,
                    svar = null,
                ),
                Opplysning(
                    opplysningId = opplysningId3,
                    seksjonsnavn = seksjon.navn,
                    opplysningsbehovId = 3,
                    type = Opplysningstype.BOOLEAN,
                    svar = BooleanSvar(opplysningId = UUID.randomUUID(), verdi = true),
                ),
            )

        every { seksjon.getOpplysningsbehov(2) } returns
            Opplysningsbehov(
                id = 2,
                tekstnøkkel = "tekstnøkkel",
                type = Opplysningstype.BOOLEAN,
            )

        every { seksjon.getOpplysningsbehov(1) } returns
            Opplysningsbehov(
                id = 1,
                tekstnøkkel = "tekstnøkkel",
                type = Opplysningstype.BOOLEAN,
            )

        val nesteSeksjon = søknadService.nesteSeksjon(søknadId)

        nesteSeksjon.seksjoner.first().besvarteOpplysninger.also { opplysninger ->
            opplysninger.size shouldBe 1
            opplysninger.first().opplysningId shouldBe opplysningId1
        }
    }

    @Test
    fun `erFullført blir true når det ikke er flere ubesvarte opplysninger i en seksjon`() {
        val søknadId = UUID.randomUUID()

        every {
            opplysningRepository.hentAlle(søknadId)
        } returns
            listOf(
                Opplysning(
                    opplysningId = UUID.randomUUID(),
                    seksjonsnavn = seksjon.navn,
                    opplysningsbehovId = 1,
                    type = Opplysningstype.BOOLEAN,
                    svar = BooleanSvar(opplysningId = UUID.randomUUID(), verdi = true),
                ),
            )

        every { seksjon.getOpplysningsbehov(any()) } returns
            Opplysningsbehov(
                id = 1,
                tekstnøkkel = "tekstnøkkel",
                type = Opplysningstype.BOOLEAN,
            )

        søknadService
            .nesteSeksjon(søknadId)
            .seksjoner
            .first()
            .erFullført shouldBe true
    }

    @Test
    fun `erFullført blir false når det finnes ubesvarte opplysninger i en seksjon`() {
        val søknadId = UUID.randomUUID()

        every {
            opplysningRepository.hentAlle(søknadId)
        } returns
            listOf(
                Opplysning(
                    opplysningId = UUID.randomUUID(),
                    seksjonsnavn = seksjon.navn,
                    opplysningsbehovId = 1,
                    type = Opplysningstype.BOOLEAN,
                    svar = null,
                ),
            )

        every { seksjon.getOpplysningsbehov(any()) } returns
            Opplysningsbehov(
                id = 1,
                tekstnøkkel = "tekstnøkkel",
                type = Opplysningstype.BOOLEAN,
            )

        søknadService
            .nesteSeksjon(søknadId)
            .seksjoner
            .first()
            .erFullført shouldBe false
    }
}
