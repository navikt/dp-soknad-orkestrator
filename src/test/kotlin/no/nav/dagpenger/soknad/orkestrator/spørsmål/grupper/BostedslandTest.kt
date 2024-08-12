package no.nav.dagpenger.soknad.orkestrator.spørsmål.grupper

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.soknad.orkestrator.spørsmål.BooleanSvar
import no.nav.dagpenger.soknad.orkestrator.spørsmål.DatoSvar
import no.nav.dagpenger.soknad.orkestrator.spørsmål.LandSvar
import no.nav.dagpenger.soknad.orkestrator.spørsmål.TekstSvar
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test

class BostedslandTest {
    private val bostedsland = Bostedsland

    @Test
    fun `neste spørsmål er null når hvilketLandBorDuI er Norge`() {
        val svar =
            LandSvar(
                spørsmålId = UUID.randomUUID(),
                verdi = "NOR",
            )

        val nesteSpørsmål = bostedsland.nesteSpørsmål(bostedsland.hvilketLandBorDuI.id, svar)

        nesteSpørsmål shouldBe null
    }

    @Test
    fun `neste spørsmål er reistTilbakeTilNorge når hvilketLandBorDuI ikke er Norge`() {
        val svar =
            LandSvar(
                spørsmålId = UUID.randomUUID(),
                verdi = "SWE",
            )

        val nesteSpørsmål = bostedsland.nesteSpørsmål(bostedsland.hvilketLandBorDuI.id, svar)

        nesteSpørsmål shouldBe bostedsland.reistTilbakeTilNorge
    }

    @Test
    fun `neste spørsmål er datoForAvreise når reistTilbakeTilNorge er true`() {
        val svar =
            BooleanSvar(
                spørsmålId = UUID.randomUUID(),
                verdi = true,
            )
        val nesteSpørsmål = bostedsland.nesteSpørsmål(bostedsland.reistTilbakeTilNorge.id, svar)

        nesteSpørsmål shouldBe bostedsland.datoForAvreise
    }

    @Test
    fun `neste spørsmål er enGangIUken når reistTilbakeTilNorge er false`() {
        val svar =
            BooleanSvar(
                spørsmålId = UUID.randomUUID(),
                verdi = false,
            )

        val nesteSpørsmål = bostedsland.nesteSpørsmål(bostedsland.reistTilbakeTilNorge.id, svar)

        nesteSpørsmål shouldBe bostedsland.enGangIUken
    }

    @Test
    fun `neste spørsmål er hvorforReisteFraNorge når datoForAvreise er besvart`() {
        val svar =
            DatoSvar(
                spørsmålId = UUID.randomUUID(),
                verdi = LocalDate.now(),
            )
        val nesteSpørsmål = bostedsland.nesteSpørsmål(bostedsland.datoForAvreise.id, svar)

        nesteSpørsmål shouldBe bostedsland.hvorforReisteFraNorge
    }

    @Test
    fun `neste spørsmål er enGangIUken når hvorforReisteFraNorge er besvart`() {
        val svar =
            TekstSvar(
                spørsmålId = UUID.randomUUID(),
                verdi = "Derfor",
            )

        val nesteSpørsmål = bostedsland.nesteSpørsmål(bostedsland.hvorforReisteFraNorge.id, svar)

        nesteSpørsmål shouldBe bostedsland.enGangIUken
    }

    @Test
    fun `neste spørsmål er null når enGangIUken er true`() {
        val svar =
            BooleanSvar(
                spørsmålId = UUID.randomUUID(),
                verdi = true,
            )

        val nesteSpørsmål = bostedsland.nesteSpørsmål(bostedsland.enGangIUken.id, svar)

        nesteSpørsmål shouldBe null
    }

    @Test
    fun `neste spørsmål er rotasjon når enGangIUken er false`() {
        val svar =
            BooleanSvar(
                spørsmålId = UUID.randomUUID(),
                verdi = false,
            )

        val nesteSpørsmål = bostedsland.nesteSpørsmål(bostedsland.enGangIUken.id, svar)

        nesteSpørsmål shouldBe bostedsland.rotasjon
    }

    @Test
    fun `neste spørsmål er null når rotasjon er besvart med true`() {
        val svar =
            BooleanSvar(
                spørsmålId = UUID.randomUUID(),
                verdi = true,
            )

        val nesteSpørsmål = bostedsland.nesteSpørsmål(bostedsland.rotasjon.id, svar)

        nesteSpørsmål shouldBe null
    }

    @Test
    fun `neste spørsmål er null når rotasjon er besvart med false`() {
        val svar =
            BooleanSvar(
                spørsmålId = UUID.randomUUID(),
                verdi = false,
            )

        val nesteSpørsmål = bostedsland.nesteSpørsmål(bostedsland.rotasjon.id, svar)

        nesteSpørsmål shouldBe null
    }

    @Test
    fun `validering kaster ikke feil når svar på hvilketLandBorDuI er gyldig`() {
        val svar =
            LandSvar(
                spørsmålId = UUID.randomUUID(),
                verdi = bostedsland.hvilketLandBorDuI.gyldigeSvar.random(),
            )

        shouldNotThrow<IllegalArgumentException> {
            bostedsland.validerSvar(
                bostedsland.hvilketLandBorDuI.id,
                svar,
            )
        }
    }

    @Test
    fun `validering kaster feil når svar på hvilketLandBorDuI er ugyldig`() {
        val svar =
            LandSvar(
                spørsmålId = UUID.randomUUID(),
                verdi = "UGYLDIG",
            )

        shouldThrow<IllegalArgumentException> {
            Bostedsland.validerSvar(Bostedsland.hvilketLandBorDuI.id, svar)
        }
    }
}
