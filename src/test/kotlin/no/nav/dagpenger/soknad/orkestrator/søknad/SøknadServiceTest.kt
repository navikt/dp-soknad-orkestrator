package no.nav.dagpenger.soknad.orkestrator.søknad

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.soknad.orkestrator.spørsmål.LandSvar
import no.nav.dagpenger.soknad.orkestrator.spørsmål.grupper.BostedslandDTOV1
import no.nav.dagpenger.soknad.orkestrator.søknad.db.InMemorySøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class SøknadServiceTest {
    private val testRapid = TestRapid()
    private val søknadRepository = mockk<SøknadRepository>(relaxed = true)
    private val inMemorySøknadRepository = InMemorySøknadRepository()
    private var søknadService =
        SøknadService(
            rapid = testRapid,
            søknadRepository = søknadRepository,
            inMemorySøknadRepository = inMemorySøknadRepository,
        )
    private val ident = "12345678901"

    @BeforeEach
    fun reset() {
        clearAllMocks()
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
        inMemorySøknadRepository.hentAlle(søknad.søknadId).first().idIGruppe shouldBe 1
        inMemorySøknadRepository.hentAlle(søknad.søknadId).first().svar shouldBe null
    }

    @Test
    fun `lagreSvar lagrer besvart spørsmål og neste spørsmål`() {
        val søknadId = UUID.randomUUID()
        val spørsmålId = UUID.randomUUID()
        val bostedslandgruppe = BostedslandDTOV1
        inMemorySøknadRepository.lagre(
            spørsmålId = spørsmålId,
            søknadId = søknadId,
            gruppeId = bostedslandgruppe.versjon,
            idIGruppe = bostedslandgruppe.hvilketLandBorDuI.idIGruppe,
            type = bostedslandgruppe.hvilketLandBorDuI.type,
            svar = null,
        )

        val svar = LandSvar(spørsmålId = spørsmålId, verdi = "SWE")

        søknadService.lagreSvar(søknadId, svar)

        inMemorySøknadRepository.hentAlle(søknadId).size shouldBe 2
        inMemorySøknadRepository.hentAlle(søknadId).find { it.spørsmålId == spørsmålId } shouldNotBe null
        inMemorySøknadRepository.hentAlle(søknadId)
            .find { it.idIGruppe == bostedslandgruppe.reistTilbakeTilNorge.idIGruppe } shouldNotBe
            null
    }

    // TODO: Godta generisk gruppe, så denne testen ikke er avhengig av Bostedsland sin logikk
    @Test
    fun `lagreBesvartSpørsmål lagrer besvartSpørsmål og nullstiller avhengigheter`() {
        val søknadId = UUID.randomUUID()
        val spørsmålId = UUID.randomUUID()
        val bostedslandgruppe = BostedslandDTOV1
        inMemorySøknadRepository.lagre(
            spørsmålId = spørsmålId,
            søknadId = søknadId,
            gruppeId = bostedslandgruppe.versjon,
            idIGruppe = bostedslandgruppe.hvilketLandBorDuI.idIGruppe,
            type = bostedslandgruppe.hvilketLandBorDuI.type,
            svar = "SWE",
        )
        inMemorySøknadRepository.lagre(
            spørsmålId = UUID.randomUUID(),
            søknadId = søknadId,
            gruppeId = bostedslandgruppe.versjon,
            idIGruppe = bostedslandgruppe.reistTilbakeTilNorge.idIGruppe,
            type = bostedslandgruppe.reistTilbakeTilNorge.type,
            svar = "true",
        )
        inMemorySøknadRepository.lagre(
            spørsmålId = UUID.randomUUID(),
            søknadId = søknadId,
            gruppeId = bostedslandgruppe.versjon,
            idIGruppe = bostedslandgruppe.datoForAvreise.idIGruppe,
            type = bostedslandgruppe.datoForAvreise.type,
            svar = null,
        )

        val svar =
            LandSvar(
                spørsmålId = spørsmålId,
                verdi = "FIN",
            )

        søknadService.lagreSvar(søknadId, svar)

        val alleSpørsmål = inMemorySøknadRepository.hentAlle(søknadId)
        alleSpørsmål.size shouldBe 2
        alleSpørsmål.find { it.spørsmålId == spørsmålId } shouldNotBe null
        alleSpørsmål.find { it.spørsmålId == spørsmålId }!!.svar shouldBe "FIN"
        alleSpørsmål.find { it.idIGruppe == bostedslandgruppe.reistTilbakeTilNorge.idIGruppe } shouldNotBe null
        alleSpørsmål.find { it.idIGruppe == bostedslandgruppe.reistTilbakeTilNorge.idIGruppe }!!.svar shouldBe null
        alleSpørsmål.find { it.idIGruppe == bostedslandgruppe.datoForAvreise.idIGruppe } shouldBe null
    }

    @Test
    fun `nesteSpørsmålgruppe henter kun besvarte spørsmål som kommer før det ubesvarte spørsmålet`() {
        val søknadId = UUID.randomUUID()
        val spørsmålId1 = UUID.randomUUID()
        val bostedslandgruppe = BostedslandDTOV1
        inMemorySøknadRepository.lagre(
            spørsmålId = spørsmålId1,
            søknadId = søknadId,
            gruppeId = bostedslandgruppe.versjon,
            idIGruppe = bostedslandgruppe.hvilketLandBorDuI.idIGruppe,
            type = bostedslandgruppe.hvilketLandBorDuI.type,
            svar = "SWE",
        )
        inMemorySøknadRepository.lagre(
            spørsmålId = UUID.randomUUID(),
            søknadId = søknadId,
            gruppeId = bostedslandgruppe.versjon,
            idIGruppe = bostedslandgruppe.reistTilbakeTilNorge.idIGruppe,
            type = bostedslandgruppe.reistTilbakeTilNorge.type,
            svar = null,
        )
        inMemorySøknadRepository.lagre(
            spørsmålId = UUID.randomUUID(),
            søknadId = søknadId,
            gruppeId = bostedslandgruppe.versjon,
            idIGruppe = bostedslandgruppe.enGangIUken.idIGruppe,
            type = bostedslandgruppe.enGangIUken.type,
            svar = "true",
        )

        val nesteSpørsmålgruppe = søknadService.nesteSpørsmålgruppe(søknadId)

        val alleSpørsmål = inMemorySøknadRepository.hentAlle(søknadId)
        alleSpørsmål.filter { it.svar != null }.size shouldBe 2
        nesteSpørsmålgruppe.besvarteSpørsmål.size shouldBe 1
        nesteSpørsmålgruppe.besvarteSpørsmål.first().id shouldBe spørsmålId1
    }
}
