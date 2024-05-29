package no.nav.dagpenger.soknad.orkestrator.søknad

import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test
import java.util.UUID

class SøknadServiceTest {
    private val testRapid = TestRapid()
    private val søknadRepository = mockk<SøknadRepository>(relaxed = true)
    private var søknadService =
        SøknadService(
            rapid = testRapid,
            søknadRepository = søknadRepository,
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
    fun `vi kan opprette en søknad`() {
        val søknad = søknadService.opprettSøknad(ident)

        verify(exactly = 1) { søknadRepository.lagre(søknad) }
    }
}
