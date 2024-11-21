package no.nav.dagpenger.soknad.orkestrator.land

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class LandfabrikkTest {
    @Test
    fun `tilLandgruppeDTO returnerer en liste med landkoder og gruppeid`() {
        val landgrupper = listOf(Landgruppe.NORGE)

        val landgruppeDTO = Landfabrikk.tilLandgruppeDTO(landgrupper)

        landgruppeDTO.size shouldBe 1
        landgruppeDTO[0].land shouldBe listOf("NOR", "SJM")
        landgruppeDTO[0].gruppeId shouldBe "gruppe.norge"
    }
}
