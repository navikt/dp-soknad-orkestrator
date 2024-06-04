package no.nav.dagpenger.soknad.orkestrator.spørsmål.grupper

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.PeriodeSvar
import java.time.LocalDate
import kotlin.test.Test

class BostedslandTest {
    private val bostedsland = Bostedsland()

    @Test
    fun `neste spørsmål er null når hvilketLandBorDuI er Norge`() {
        bostedsland.hvilketLandBorDuI.svar = "NOR"
        val nesteSpørsmål = bostedsland.opprettNesteSpørsmål(bostedsland.hvilketLandBorDuI)

        nesteSpørsmål shouldBe null
    }

    // TODO: Oppdater test når vi har implementert riktig logikk for dette spørsmålet
    @Test
    fun `neste spørsmål er reistTilbakeTilNorge når hvilketLandBorDuI ikke er Norge`() {
        bostedsland.hvilketLandBorDuI.svar = "SWE"
        val nesteSpørsmål = bostedsland.opprettNesteSpørsmål(bostedsland.hvilketLandBorDuI)

        nesteSpørsmål shouldBe bostedsland.reistTilbakeTilNorge
    }

    @Test
    fun `neste spørsmål er datoForAvreise når reistTilbakeTilNorge er true`() {
        bostedsland.reistTilbakeTilNorge.svar = true
        val nesteSpørsmål = bostedsland.opprettNesteSpørsmål(bostedsland.reistTilbakeTilNorge)

        nesteSpørsmål shouldBe bostedsland.datoForAvreise
    }

    @Test
    fun `neste spørsmål er enGangIUken når reistTilbakeTilNorge er false`() {
        bostedsland.reistTilbakeTilNorge.svar = false
        val nesteSpørsmål = bostedsland.opprettNesteSpørsmål(bostedsland.reistTilbakeTilNorge)

        nesteSpørsmål shouldBe bostedsland.enGangIUken
    }

    @Test
    fun `neste spørsmål er hvorforReisteFraNorge når datoForAvreise er besvart`() {
        bostedsland.datoForAvreise.svar = PeriodeSvar(LocalDate.now(), LocalDate.now())
        val nesteSpørsmål = bostedsland.opprettNesteSpørsmål(bostedsland.datoForAvreise)

        nesteSpørsmål shouldBe bostedsland.hvorforReisteFraNorge
    }

    @Test
    fun `neste spørsmål er enGangIUken når hvorforReisteFraNorger er besvart`() {
        bostedsland.hvorforReisteFraNorge.svar = "Derfor"
        val nesteSpørsmål = bostedsland.opprettNesteSpørsmål(bostedsland.hvorforReisteFraNorge)

        nesteSpørsmål shouldBe bostedsland.enGangIUken
    }

    @Test
    fun `neste spørsmål er null når enGangIUken er true`() {
        bostedsland.enGangIUken.svar = true
        val nesteSpørsmål = bostedsland.opprettNesteSpørsmål(bostedsland.enGangIUken)

        nesteSpørsmål shouldBe null
    }

    @Test
    fun `neste spørsmål er rotasjon når enGangIUken er false`() {
        bostedsland.enGangIUken.svar = false
        val nesteSpørsmål = bostedsland.opprettNesteSpørsmål(bostedsland.enGangIUken)

        nesteSpørsmål shouldBe bostedsland.rotasjon
    }

    @Test
    fun `neste spørsmål er null når rotasjon er besvart`() {
        bostedsland.rotasjon.svar = true
        bostedsland.opprettNesteSpørsmål(bostedsland.rotasjon) shouldBe null

        bostedsland.rotasjon.svar = false
        bostedsland.opprettNesteSpørsmål(bostedsland.rotasjon) shouldBe null
    }
}
