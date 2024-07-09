package no.nav.dagpenger.soknad.orkestrator.spørsmål.grupper

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.PeriodeSvar
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test

class BostedslandTest {
    private val bostedsland = BostedslandDTOV1

    @Test
    fun `neste spørsmål er null når hvilketLandBorDuI er Norge`() {
        val besvartSpørsmål =
            bostedsland.hvilketLandBorDuI.toSporsmalDTO(
                spørsmålId = UUID.randomUUID(),
                svar = "NOR",
            )

        val nesteSpørsmål = bostedsland.nesteSpørsmål(besvartSpørsmål)

        nesteSpørsmål shouldBe null
    }

    // TODO: Oppdater test når vi har implementert riktig logikk for dette spørsmålet
    @Test
    fun `neste spørsmål er reistTilbakeTilNorge når hvilketLandBorDuI ikke er Norge`() {
        val besvartSpørsmål =
            bostedsland.hvilketLandBorDuI.toSporsmalDTO(
                spørsmålId = UUID.randomUUID(),
                svar = "SWE",
            )

        val nesteSpørsmål = bostedsland.nesteSpørsmål(besvartSpørsmål)

        nesteSpørsmål shouldBe bostedsland.reistTilbakeTilNorge
    }

    @Test
    fun `neste spørsmål er datoForAvreise når reistTilbakeTilNorge er true`() {
        val besvartSpørsmål =
            bostedsland.reistTilbakeTilNorge.toSporsmalDTO(
                spørsmålId = UUID.randomUUID(),
                svar = "true",
            )

        val nesteSpørsmål = bostedsland.nesteSpørsmål(besvartSpørsmål)

        nesteSpørsmål shouldBe bostedsland.datoForAvreise
    }

    @Test
    fun `neste spørsmål er enGangIUken når reistTilbakeTilNorge er false`() {
        val besvartSpørsmål =
            bostedsland.reistTilbakeTilNorge.toSporsmalDTO(
                spørsmålId = UUID.randomUUID(),
                svar = "false",
            )

        val nesteSpørsmål = bostedsland.nesteSpørsmål(besvartSpørsmål)

        nesteSpørsmål shouldBe bostedsland.enGangIUken
    }

    @Test
    fun `neste spørsmål er hvorforReisteFraNorge når datoForAvreise er besvart`() {
        val besvartSpørsmål =
            bostedsland.datoForAvreise.toSporsmalDTO(
                spørsmålId = UUID.randomUUID(),
                svar = objectMapper.writeValueAsString(PeriodeSvar(LocalDate.now(), LocalDate.now())),
            )

        val nesteSpørsmål = bostedsland.nesteSpørsmål(besvartSpørsmål)

        nesteSpørsmål shouldBe bostedsland.hvorforReisteFraNorge
    }

    @Test
    fun `neste spørsmål er enGangIUken når hvorforReisteFraNorger er besvart`() {
        val besvartSpørsmål =
            bostedsland.hvorforReisteFraNorge.toSporsmalDTO(
                spørsmålId = UUID.randomUUID(),
                svar = "Derfor",
            )

        val nesteSpørsmål = bostedsland.nesteSpørsmål(besvartSpørsmål)

        nesteSpørsmål shouldBe bostedsland.enGangIUken
    }

    @Test
    fun `neste spørsmål er null når enGangIUken er true`() {
        val besvartSpørsmål =
            bostedsland.enGangIUken.toSporsmalDTO(
                spørsmålId = UUID.randomUUID(),
                svar = "true",
            )

        val nesteSpørsmål = bostedsland.nesteSpørsmål(besvartSpørsmål)

        nesteSpørsmål shouldBe null
    }

    @Test
    fun `neste spørsmål er rotasjon når enGangIUken er false`() {
        val besvartSpørsmål =
            bostedsland.enGangIUken.toSporsmalDTO(
                spørsmålId = UUID.randomUUID(),
                svar = "false",
            )

        val nesteSpørsmål = bostedsland.nesteSpørsmål(besvartSpørsmål)

        nesteSpørsmål shouldBe bostedsland.rotasjon
    }

    @Test
    fun `neste spørsmål er null når rotasjon er besvart med true`() {
        val besvartSpørsmål =
            bostedsland.rotasjon.toSporsmalDTO(
                spørsmålId = UUID.randomUUID(),
                svar = "true",
            )

        bostedsland.nesteSpørsmål(besvartSpørsmål) shouldBe null
    }

    @Test
    fun `neste spørsmål er null når rotasjon er besvart med false`() {
        val besvartSpørsmål =
            bostedsland.rotasjon.toSporsmalDTO(
                spørsmålId = UUID.randomUUID(),
                svar = "false",
            )

        bostedsland.nesteSpørsmål(besvartSpørsmål) shouldBe null
    }
}
