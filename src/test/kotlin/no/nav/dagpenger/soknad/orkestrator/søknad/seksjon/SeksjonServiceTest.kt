package no.nav.dagpenger.soknad.orkestrator.søknad.seksjon

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.UUID
import kotlin.test.Test

class SeksjonServiceTest {
    private val søknadId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
    private val ident = "12345678903"
    var seksjonRepositoryMock = mockk<SeksjonRepository>()
    var seksjonService = SeksjonService(seksjonRepositoryMock)

    @Test
    fun `hentAlleSeksjonerMedSeksjonIdSomNøkkel returnerer map med seksjonId som key og seksjonsvar som value`() {
        every {
            seksjonRepositoryMock.hentSeksjoner(any(), any())
        } returns
            listOf(
                Seksjon(
                    "egen-naring",
                    """{"seksjonId":"egen-naring","seksjonsvar":{"driverDuEgenNæringsvirksomhet":"nei","næringsvirksomheter":null,"driverDuEgetGårdsbruk":"nei","gårdsbruk":null},"versjon":1}""",
                ),
                Seksjon(
                    "verneplikt",
                    """{"seksjonId":"verneplikt","seksjonsvar":{"avtjentVerneplikt":"nei","dokumentasjonskrav":"null"},"versjon":1}""",
                ),
            )

        val seksjonerMap = seksjonService.hentAlleSeksjonerMedSeksjonIdSomNøkkel(ident, søknadId)
        verify { seksjonRepositoryMock.hentSeksjoner(ident, søknadId) }
        assert(seksjonerMap.size == 2)
        seksjonerMap["egen-naring"] shouldBe
            """{"driverDuEgenNæringsvirksomhet":"nei","næringsvirksomheter":null,"driverDuEgetGårdsbruk":"nei","gårdsbruk":null}"""
        seksjonerMap["verneplikt"] shouldBe """{"avtjentVerneplikt":"nei","dokumentasjonskrav":"null"}"""
    }

    @Test
    fun `hentAlleSeksjonerMedSeksjonIdSomNøkkel returnerer tom map om ingen svar finnes`() {
        every {
            seksjonRepositoryMock.hentSeksjoner(any(), any())
        } returns
            listOf()

        val seksjonerMap = seksjonService.hentAlleSeksjonerMedSeksjonIdSomNøkkel(ident, søknadId)
        verify { seksjonRepositoryMock.hentSeksjoner(ident, søknadId) }
        assert(seksjonerMap.size == 0)
    }
}
