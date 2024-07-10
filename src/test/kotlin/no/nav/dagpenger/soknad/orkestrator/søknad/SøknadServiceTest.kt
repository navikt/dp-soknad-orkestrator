package no.nav.dagpenger.soknad.orkestrator.søknad

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.soknad.orkestrator.spørsmål.grupper.BostedslandDTOV1
import no.nav.dagpenger.soknad.orkestrator.spørsmål.grupper.toSporsmalDTO
import no.nav.dagpenger.soknad.orkestrator.søknad.db.InMemorySøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.helse.rapids_rivers.testsupport.TestRapid
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
    fun `lagreBesvartSpørsmål lagrer besvart spørsmål og neste spørsmål`() {
        val søknadId = UUID.randomUUID()
        val spørsmålId = UUID.randomUUID()
        val bostedslandgruppe = BostedslandDTOV1
        inMemorySøknadRepository.lagre(
            spørsmålId = spørsmålId,
            søknadId = søknadId,
            gruppeId = bostedslandgruppe.versjon,
            idIGruppe = bostedslandgruppe.hvilketLandBorDuI.idIGruppe,
            svar = null,
        )
        val besvartSpørsmål =
            bostedslandgruppe.hvilketLandBorDuI.toSporsmalDTO(
                spørsmålId = spørsmålId,
                svar = "SWE",
            )

        søknadService.lagreBesvartSpørsmål(søknadId, besvartSpørsmål)

        inMemorySøknadRepository.hentAlle(søknadId).size shouldBe 2
        inMemorySøknadRepository.hentAlle(søknadId).find { it.spørsmålId == spørsmålId } shouldNotBe null
        inMemorySøknadRepository.hentAlle(søknadId).find { it.idIGruppe == bostedslandgruppe.reistTilbakeTilNorge.idIGruppe } shouldNotBe
            null
    }

    @Test
    fun `lagreBesvartSpørsmål lagrer besvartSpørsmål og nullstiller avhengigheter`() {
        val søknadId = UUID.randomUUID()
        val spørsmålId1 = UUID.randomUUID()
        val bostedslandgruppe = BostedslandDTOV1
        inMemorySøknadRepository.lagre(
            spørsmålId = spørsmålId1,
            søknadId = søknadId,
            gruppeId = bostedslandgruppe.versjon,
            idIGruppe = bostedslandgruppe.hvilketLandBorDuI.idIGruppe,
            svar = "SWE",
        )
        inMemorySøknadRepository.lagre(
            spørsmålId = UUID.randomUUID(),
            søknadId = søknadId,
            gruppeId = bostedslandgruppe.versjon,
            idIGruppe = bostedslandgruppe.reistTilbakeTilNorge.idIGruppe,
            svar = "true",
        )
        inMemorySøknadRepository.lagre(
            spørsmålId = UUID.randomUUID(),
            søknadId = søknadId,
            gruppeId = bostedslandgruppe.versjon,
            idIGruppe = bostedslandgruppe.datoForAvreise.idIGruppe,
            svar = null,
        )

        val besvartSpørsmål =
            bostedslandgruppe.hvilketLandBorDuI.toSporsmalDTO(
                spørsmålId = spørsmålId1,
                svar = "FIN",
            )
        søknadService.lagreBesvartSpørsmål(søknadId, besvartSpørsmål)

        val alleSpørsmål = inMemorySøknadRepository.hentAlle(søknadId)
        alleSpørsmål.size shouldBe 2
        alleSpørsmål.find { it.spørsmålId == spørsmålId1 } shouldNotBe null
        alleSpørsmål.find { it.spørsmålId == spørsmålId1 }!!.svar shouldBe "FIN"
        alleSpørsmål.find { it.idIGruppe == bostedslandgruppe.reistTilbakeTilNorge.idIGruppe } shouldNotBe null
        alleSpørsmål.find { it.idIGruppe == bostedslandgruppe.reistTilbakeTilNorge.idIGruppe }!!.svar shouldBe null
        alleSpørsmål.find { it.idIGruppe == bostedslandgruppe.datoForAvreise.idIGruppe } shouldBe null
    }
}
