package no.nav.dagpenger.soknad.orkestrator.søknad

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import no.nav.dagpenger.soknad.orkestrator.opplysning.BooleanSvar
import no.nav.dagpenger.soknad.orkestrator.opplysning.LandSvar
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysningsbehov
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysningstype
import no.nav.dagpenger.soknad.orkestrator.opplysning.Svar
import no.nav.dagpenger.soknad.orkestrator.opplysning.db.OpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.opplysning.grupper.Seksjon
import no.nav.dagpenger.soknad.orkestrator.opplysning.grupper.Seksjonsnavn
import no.nav.dagpenger.soknad.orkestrator.opplysning.grupper.getSeksjon
import no.nav.dagpenger.soknad.orkestrator.søknad.db.InMemorySøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.db.Spørsmål
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class SøknadServiceTest {
    private val testRapid = TestRapid()
    private val søknadRepository = mockk<SøknadRepository>(relaxed = true)
    private val opplysningRepository = mockk<OpplysningRepository>(relaxed = true)
    private val seksjon = mockk<Seksjon>(relaxed = true)
    private val inMemorySøknadRepository = InMemorySøknadRepository()
    private var søknadService =
        SøknadService(
            søknadRepository = søknadRepository,
            inMemorySøknadRepository = inMemorySøknadRepository,
            opplysningRepository = opplysningRepository,
        ).also { it.setRapidsConnection(testRapid) }
    private val ident = "12345678901"
    private val seksjonPath = "no.nav.dagpenger.soknad.orkestrator.opplysning.grupper.SeksjonKt"

    @BeforeEach
    fun setup() {
        mockkStatic(seksjonPath)
        every { getSeksjon(Seksjonsnavn.BOSTEDSLAND) } returns seksjon
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

        søknadService.søknadFinnes(UUID.randomUUID()) shouldBe true
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
    fun `oppretting av søknad oppretter også første spørsmål i søknaden`() {
        val søknad = søknadService.opprettSøknad(ident)

        verify(exactly = 1) { søknadRepository.lagre(søknad) }
        inMemorySøknadRepository.hentAlle(søknad.søknadId).size shouldBe 1
        inMemorySøknadRepository.hentAlle(søknad.søknadId).first().gruppespørsmålId shouldBe 1
        inMemorySøknadRepository.hentAlle(søknad.søknadId).first().svar shouldBe null
    }

    @Test
    fun `lagreSvar lagrer besvart spørsmål og neste spørsmål`() {
        val søknadId = UUID.randomUUID()
        val spørsmålId = UUID.randomUUID()
        every { seksjon.nesteOpplysningsbehov(any<Svar<*>>(), any<Int>()) } returns
            Opplysningsbehov(
                id = 2,
                tekstnøkkel = "spm2",
                type = Opplysningstype.TEKST,
                gyldigeSvar = emptyList(),
            )
        inMemorySøknadRepository.lagreTestSpørsmål(
            søknadId = søknadId,
            spørsmålId = spørsmålId,
        )

        val svar = BooleanSvar(opplysningId = spørsmålId, verdi = true)
        søknadService.håndterSvar(søknadId, svar)

        inMemorySøknadRepository.hentAlle(søknadId).size shouldBe 2
        inMemorySøknadRepository.hentAlle(søknadId).find { it.spørsmålId == spørsmålId } shouldNotBe null
        inMemorySøknadRepository
            .hentAlle(søknadId)
            .find { it.gruppespørsmålId == 2 } shouldNotBe null
    }

    @Test
    fun `lagreBesvartSpørsmål lagrer besvartSpørsmål og nullstiller avhengigheter`() {
        val søknadId = UUID.randomUUID()
        val spørsmålId1 = UUID.randomUUID()
        val gruppespørsmålId1 = 1
        val gruppespørsmålId2 = 2
        every { seksjon.avhengigheter(gruppespørsmålId1) } returns listOf(gruppespørsmålId2)
        every { seksjon.nesteOpplysningsbehov(any<Svar<*>>(), any<Int>()) } returns null
        inMemorySøknadRepository.lagreTestSpørsmål(
            søknadId = søknadId,
            spørsmålId = spørsmålId1,
            gruppespørsmålId = gruppespørsmålId1,
            type = Opplysningstype.LAND,
            svar = LandSvar(opplysningId = spørsmålId1, verdi = "NED"),
        )
        inMemorySøknadRepository.lagreTestSpørsmål(
            søknadId = søknadId,
            gruppespørsmålId = gruppespørsmålId2,
            svar = BooleanSvar(opplysningId = spørsmålId1, verdi = true),
        )

        val svar = LandSvar(opplysningId = spørsmålId1, verdi = "OPP")
        søknadService.håndterSvar(søknadId, svar)

        val alleSpørsmål = inMemorySøknadRepository.hentAlle(søknadId)
        alleSpørsmål.size shouldBe 1
        alleSpørsmål.find { it.spørsmålId == spørsmålId1 } shouldNotBe null
        alleSpørsmål.find { it.spørsmålId == spørsmålId1 }?.svar?.verdi shouldBe "OPP"
        alleSpørsmål.find { it.gruppespørsmålId == gruppespørsmålId2 }?.svar shouldBe null
    }

    @Test
    fun `lagreBesvartSpørsmål lagrer besvartSpørsmål uten å nullstille ikke-avhengigheter`() {
        val søknadId = UUID.randomUUID()
        val spørsmålId1 = UUID.randomUUID()
        val gruppespørsmålId1 = 1
        val gruppespørsmålId2 = 2
        every { seksjon.avhengigheter(gruppespørsmålId1) } returns emptyList()
        every { seksjon.nesteOpplysningsbehov(any<Svar<*>>(), any<Int>()) } returns null
        inMemorySøknadRepository.lagreTestSpørsmål(
            søknadId = søknadId,
            spørsmålId = spørsmålId1,
            gruppespørsmålId = gruppespørsmålId1,
            type = Opplysningstype.LAND,
            svar = LandSvar(opplysningId = spørsmålId1, verdi = "NED"),
        )
        inMemorySøknadRepository.lagreTestSpørsmål(
            søknadId = søknadId,
            gruppespørsmålId = gruppespørsmålId2,
            svar = BooleanSvar(opplysningId = spørsmålId1, verdi = true),
        )

        val svar = LandSvar(opplysningId = spørsmålId1, verdi = "OPP")
        søknadService.håndterSvar(søknadId, svar)

        val alleSpørsmål = inMemorySøknadRepository.hentAlle(søknadId)
        alleSpørsmål.size shouldBe 2
        alleSpørsmål.find { it.spørsmålId == spørsmålId1 } shouldNotBe null
        alleSpørsmål.find { it.spørsmålId == spørsmålId1 }?.svar?.verdi shouldBe "OPP"
        alleSpørsmål.find { it.gruppespørsmålId == gruppespørsmålId2 }?.svar?.verdi shouldBe true
    }

    @Test
    fun `nesteSpørsmålgruppe henter kun besvarte spørsmål som kommer før det ubesvarte spørsmålet`() {
        val søknadId = UUID.randomUUID()
        val spørsmålId1 = UUID.randomUUID()
        val spørsmålId3 = UUID.randomUUID()
        every { seksjon.getOpplysningsbehov(any()) } returns
            Opplysningsbehov(
                id = 99,
                tekstnøkkel = "spm",
                type = Opplysningstype.BOOLEAN,
                gyldigeSvar = emptyList(),
            )
        inMemorySøknadRepository.lagreTestSpørsmål(
            søknadId = søknadId,
            spørsmålId = spørsmålId1,
            gruppespørsmålId = 1,
            svar = BooleanSvar(opplysningId = spørsmålId1, verdi = true),
        )
        inMemorySøknadRepository.lagreTestSpørsmål(
            søknadId = søknadId,
            gruppespørsmålId = 2,
            svar = null,
        )
        inMemorySøknadRepository.lagreTestSpørsmål(
            søknadId = søknadId,
            spørsmålId = spørsmålId3,
            gruppespørsmålId = 3,
            svar = BooleanSvar(opplysningId = spørsmålId1, verdi = false),
        )

        val nesteSpørsmålgruppe = søknadService.nesteSpørsmålgruppe(søknadId)

        val alleSpørsmål = inMemorySøknadRepository.hentAlle(søknadId)
        alleSpørsmål.filter { it.svar != null }.size shouldBe 2
        nesteSpørsmålgruppe.besvarteSpørsmål.size shouldBe 1
        nesteSpørsmålgruppe.besvarteSpørsmål.first().id shouldBe spørsmålId1
    }

    @Test
    fun `erFullført blir true når det ikke er flere ubesvarte spørsmål i en spørsmålsgruppe`() {
        val søknadId = UUID.randomUUID()
        val spørsmålId = UUID.randomUUID()
        every { seksjon.getOpplysningsbehov(any()) } returns
            Opplysningsbehov(
                id = 99,
                tekstnøkkel = "spm",
                type = Opplysningstype.BOOLEAN,
                gyldigeSvar = emptyList(),
            )

        inMemorySøknadRepository.lagreTestSpørsmål(
            søknadId = søknadId,
            spørsmålId = spørsmålId,
            gruppespørsmålId = 1,
            svar = null,
        )

        søknadService.nesteSpørsmålgruppe(søknadId).erFullført shouldBe false

        inMemorySøknadRepository.lagreSvar(søknadId, BooleanSvar(opplysningId = spørsmålId, verdi = true))

        søknadService.nesteSpørsmålgruppe(søknadId).erFullført shouldBe true
    }
}

fun InMemorySøknadRepository.lagreTestSpørsmål(
    søknadId: UUID = UUID.randomUUID(),
    spørsmålId: UUID = UUID.randomUUID(),
    gruppenavn: Seksjonsnavn = Seksjonsnavn.BOSTEDSLAND,
    gruppespørsmålId: Int = 1,
    type: Opplysningstype = Opplysningstype.BOOLEAN,
    svar: Svar<*>? = null,
) {
    val spørsmål =
        Spørsmål(
            spørsmålId = spørsmålId,
            gruppenavn = gruppenavn,
            gruppespørsmålId = gruppespørsmålId,
            type = type,
            svar = svar,
        )
    lagre(
        søknadId = søknadId,
        spørsmål = spørsmål,
    )
}
