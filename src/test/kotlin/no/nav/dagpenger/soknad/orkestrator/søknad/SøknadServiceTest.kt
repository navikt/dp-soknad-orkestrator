package no.nav.dagpenger.soknad.orkestrator.søknad

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import no.nav.dagpenger.soknad.orkestrator.spørsmål.BooleanSvar
import no.nav.dagpenger.soknad.orkestrator.spørsmål.GrunnleggendeSpørsmål
import no.nav.dagpenger.soknad.orkestrator.spørsmål.LandSvar
import no.nav.dagpenger.soknad.orkestrator.spørsmål.SpørsmålType
import no.nav.dagpenger.soknad.orkestrator.spørsmål.Svar
import no.nav.dagpenger.soknad.orkestrator.spørsmål.grupper.Spørsmålgruppe
import no.nav.dagpenger.soknad.orkestrator.spørsmål.grupper.Spørsmålgruppenavn
import no.nav.dagpenger.soknad.orkestrator.spørsmål.grupper.getSpørsmålgruppe
import no.nav.dagpenger.soknad.orkestrator.søknad.db.InMemorySøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.db.Spørsmål
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class SøknadServiceTest {
    private val testRapid = TestRapid()
    private val søknadRepository = mockk<SøknadRepository>(relaxed = true)
    private val spørsmålgruppe = mockk<Spørsmålgruppe>(relaxed = true)
    private val inMemorySøknadRepository = InMemorySøknadRepository()
    private var søknadService =
        SøknadService(
            rapid = testRapid,
            søknadRepository = søknadRepository,
            inMemorySøknadRepository = inMemorySøknadRepository,
        )
    private val ident = "12345678901"
    private val spørsmålgruppePath = "no.nav.dagpenger.soknad.orkestrator.spørsmål.grupper.SpørsmålgruppeKt"

    @BeforeEach
    fun setup() {
        mockkStatic(spørsmålgruppePath)
        every { getSpørsmålgruppe(Spørsmålgruppenavn.BOSTEDSLAND) } returns spørsmålgruppe
        every { spørsmålgruppe.navn } returns Spørsmålgruppenavn.BOSTEDSLAND
    }

    @AfterEach
    fun reset() {
        clearMocks(søknadRepository, spørsmålgruppe)
        unmockkStatic(spørsmålgruppePath)
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
        every { spørsmålgruppe.nesteSpørsmål(any<Svar<*>>(), any<Int>()) } returns
            GrunnleggendeSpørsmål(
                id = 2,
                tekstnøkkel = "spm2",
                type = SpørsmålType.TEKST,
                gyldigeSvar = emptyList(),
            )
        inMemorySøknadRepository.lagreTestSpørsmål(
            søknadId = søknadId,
            spørsmålId = spørsmålId,
        )

        val svar = BooleanSvar(spørsmålId = spørsmålId, verdi = true)
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
        every { spørsmålgruppe.avhengigheter(gruppespørsmålId1) } returns listOf(gruppespørsmålId2)
        every { spørsmålgruppe.nesteSpørsmål(any<Svar<*>>(), any<Int>()) } returns null
        inMemorySøknadRepository.lagreTestSpørsmål(
            søknadId = søknadId,
            spørsmålId = spørsmålId1,
            gruppespørsmålId = gruppespørsmålId1,
            type = SpørsmålType.LAND,
            svar = LandSvar(spørsmålId = spørsmålId1, verdi = "NED"),
        )
        inMemorySøknadRepository.lagreTestSpørsmål(
            søknadId = søknadId,
            gruppespørsmålId = gruppespørsmålId2,
            svar = BooleanSvar(spørsmålId = spørsmålId1, verdi = true),
        )

        val svar = LandSvar(spørsmålId = spørsmålId1, verdi = "OPP")
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
        every { spørsmålgruppe.avhengigheter(gruppespørsmålId1) } returns emptyList()
        every { spørsmålgruppe.nesteSpørsmål(any<Svar<*>>(), any<Int>()) } returns null
        inMemorySøknadRepository.lagreTestSpørsmål(
            søknadId = søknadId,
            spørsmålId = spørsmålId1,
            gruppespørsmålId = gruppespørsmålId1,
            type = SpørsmålType.LAND,
            svar = LandSvar(spørsmålId = spørsmålId1, verdi = "NED"),
        )
        inMemorySøknadRepository.lagreTestSpørsmål(
            søknadId = søknadId,
            gruppespørsmålId = gruppespørsmålId2,
            svar = BooleanSvar(spørsmålId = spørsmålId1, verdi = true),
        )

        val svar = LandSvar(spørsmålId = spørsmålId1, verdi = "OPP")
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
        every { spørsmålgruppe.getSpørsmål(any()) } returns
            GrunnleggendeSpørsmål(
                id = 99,
                tekstnøkkel = "spm",
                type = SpørsmålType.BOOLEAN,
                gyldigeSvar = emptyList(),
            )
        inMemorySøknadRepository.lagreTestSpørsmål(
            søknadId = søknadId,
            spørsmålId = spørsmålId1,
            gruppespørsmålId = 1,
            svar = BooleanSvar(spørsmålId = spørsmålId1, verdi = true),
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
            svar = BooleanSvar(spørsmålId = spørsmålId1, verdi = false),
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
        every { spørsmålgruppe.getSpørsmål(any()) } returns
            GrunnleggendeSpørsmål(
                id = 99,
                tekstnøkkel = "spm",
                type = SpørsmålType.BOOLEAN,
                gyldigeSvar = emptyList(),
            )

        inMemorySøknadRepository.lagreTestSpørsmål(
            søknadId = søknadId,
            spørsmålId = spørsmålId,
            gruppespørsmålId = 1,
            svar = null,
        )

        søknadService.nesteSpørsmålgruppe(søknadId).erFullført shouldBe false

        inMemorySøknadRepository.lagreSvar(søknadId, BooleanSvar(spørsmålId = spørsmålId, verdi = true))

        søknadService.nesteSpørsmålgruppe(søknadId).erFullført shouldBe true
    }
}

fun InMemorySøknadRepository.lagreTestSpørsmål(
    søknadId: UUID = UUID.randomUUID(),
    spørsmålId: UUID = UUID.randomUUID(),
    gruppenavn: Spørsmålgruppenavn = Spørsmålgruppenavn.BOSTEDSLAND,
    gruppespørsmålId: Int = 1,
    type: SpørsmålType = SpørsmålType.BOOLEAN,
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
