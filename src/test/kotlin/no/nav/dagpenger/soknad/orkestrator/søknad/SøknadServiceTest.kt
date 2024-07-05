package no.nav.dagpenger.soknad.orkestrator.søknad

import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.soknad.orkestrator.spørsmål.grupper.BostedslandDTO
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
        inMemorySøknadRepository.hentAlle(søknad.søknadId).first().tekstnøkkel shouldBe "bostedsland.hvilket-land-bor-du-i"
    }

    @Test
    fun `lagreBesvartSpørsmål should store answered question and fetch next question if available`() {
        val søknadId = UUID.randomUUID()
        val bostedslandgruppe = BostedslandDTO()
        inMemorySøknadRepository.lagre(søknadId, bostedslandgruppe.hvilketLandBorDuI)
        val besvartSpørsmål = bostedslandgruppe.hvilketLandBorDuI.copy(svar = "SWE")

        søknadService.lagreBesvartSpørsmål(søknadId, besvartSpørsmål)

        inMemorySøknadRepository.hentAlle(søknadId).size shouldBe 2
        inMemorySøknadRepository.hentAlle(søknadId) shouldContain besvartSpørsmål
        inMemorySøknadRepository.hentAlle(
            søknadId,
        ).find { it.tekstnøkkel == bostedslandgruppe.reistTilbakeTilNorge.tekstnøkkel } shouldNotBe null
    }
}
