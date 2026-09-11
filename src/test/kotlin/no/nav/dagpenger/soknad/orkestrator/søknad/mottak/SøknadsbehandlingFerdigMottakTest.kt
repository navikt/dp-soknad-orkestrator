package no.nav.dagpenger.soknad.orkestrator.søknad.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.dagpenger.soknad.orkestrator.søknad.Status
import no.nav.dagpenger.soknad.orkestrator.søknad.Søknad
import no.nav.dagpenger.soknad.orkestrator.søknad.SøknadStatus
import no.nav.dagpenger.soknad.orkestrator.søknad.Tilstand
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadStatusRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.mottak.SøknadsbehandlingFerdigMottak.Companion.BEHOV
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test

class SøknadsbehandlingFerdigMottakTest {
    private val søknadId = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private val behandlingId = "3fa85f64-5717-4562-b3fc-2c963f66afa6"
    private val ident = "string"
    private val rapidsConnection = TestRapid()
    private val søknadRepository = mockk<SøknadRepository>(relaxed = true)
    private val søknadStatusRepository = mockk<SøknadStatusRepository>(relaxed = true)

    init {
        SøknadsbehandlingFerdigMottak(rapidsConnection, søknadRepository, søknadStatusRepository)
    }

    @BeforeTest
    fun setup() {
        clearMocks(søknadRepository, søknadStatusRepository)
        rapidsConnection.reset()
    }

    @Test
    fun `onPacket leser melding og lagrer søknadstatus`() {
        every { søknadRepository.hent(søknadId) } returns
            Søknad(søknadId = søknadId, ident = ident, tilstand = Tilstand.INNSENDT)

        rapidsConnection.sendTestMessage(søknadsbehandlingFerdigMelding())

        val lagretStatus = slot<SøknadStatus>()
        verify { søknadStatusRepository.lagre(capture(lagretStatus)) }

        with(lagretStatus.captured) {
            this.søknadId shouldBe this@SøknadsbehandlingFerdigMottakTest.søknadId
            this.ident shouldBe this@SøknadsbehandlingFerdigMottakTest.ident
            this.behandlingId shouldBe this@SøknadsbehandlingFerdigMottakTest.behandlingId
            this.førteTil shouldBe Status.Innvilgelse
            this.rettighetsperioder shouldBe
                """[{"fraOgMed":"2026-09-08","tilOgMed":"2026-09-08","harRett":true,"opprinnelse":"Ny"}]"""
        }
    }

    @Test
    fun `onPacket lagrer ikke om søknaden ikke finnes i basen`() {
        every { søknadRepository.hent(søknadId) } returns null

        rapidsConnection.sendTestMessage(søknadsbehandlingFerdigMelding())

        verify(exactly = 0) { søknadStatusRepository.lagre(any()) }
    }

    @Test
    fun `onPacket med ident som ikke tilhører søknaden lagrer ikke status`() {
        every { søknadRepository.hent(søknadId) } returns
            Søknad(søknadId = søknadId, ident = "12345678901", tilstand = Tilstand.INNSENDT)

        shouldNotThrowAny {
            rapidsConnection.sendTestMessage(søknadsbehandlingFerdigMelding())
        }

        verify(exactly = 0) { søknadStatusRepository.lagre(any()) }
    }

    @Test
    fun `onPacket med ukjent verdi i førteTil lagrer status som Ukjent`() {
        every { søknadRepository.hent(søknadId) } returns
            Søknad(søknadId = søknadId, ident = ident, tilstand = Tilstand.INNSENDT)

        shouldNotThrowAny {
            rapidsConnection.sendTestMessage(søknadsbehandlingFerdigMelding(førteTil = "Tullestatus"))
        }

        val lagretStatus = slot<SøknadStatus>()
        verify { søknadStatusRepository.lagre(capture(lagretStatus)) }
        lagretStatus.captured.førteTil shouldBe Status.Ukjent
    }

    @Test
    fun `onPacket ignorerer melding med annet event_name`() {
        rapidsConnection.sendTestMessage(søknadsbehandlingFerdigMelding(eventName = "et_annet_event"))

        verify(exactly = 0) { søknadStatusRepository.lagre(any()) }
    }

    @Test
    fun `onPacket ignorerer melding som mangler påkrevde nøkler`() {
        rapidsConnection.sendTestMessage(
            """
            {
              "@event_name": "$BEHOV",
              "søknadId": "$søknadId"
            }
            """.trimIndent(),
        )

        verify(exactly = 0) { søknadStatusRepository.lagre(any()) }
    }

    private fun søknadsbehandlingFerdigMelding(
        eventName: String = BEHOV,
        førteTil: String = "Innvilgelse",
    ) = """
        {
          "@event_name": "$eventName",
          "ident": "$ident",
          "behandlingId": "$behandlingId",
          "søknadId": "$søknadId",
          "førteTil": "$førteTil",
          "rettighetsperioder": [
            {
              "fraOgMed": "2026-09-08",
              "tilOgMed": "2026-09-08",
              "harRett": true,
              "opprinnelse": "Ny"
            }
          ]
        }
        """.trimIndent()
}
