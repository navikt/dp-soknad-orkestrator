package no.nav.dagpenger.soknad.orkestrator.spørsmål.grupper

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.soknad.orkestrator.spørsmål.BooleanSvar
import no.nav.dagpenger.soknad.orkestrator.spørsmål.DatoSvar
import no.nav.dagpenger.soknad.orkestrator.spørsmål.LandSvar
import no.nav.dagpenger.soknad.orkestrator.spørsmål.SpørsmålType
import no.nav.dagpenger.soknad.orkestrator.spørsmål.TekstSvar
import no.nav.dagpenger.soknad.orkestrator.søknad.db.Spørsmål
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test

class BostedslandTest {
    @Test
    fun `neste spørsmål er null når hvilketLandBorDuI er Norge`() {
        val besvartSpørsmål =
            Spørsmål(
                spørsmålId = UUID.randomUUID(),
                gruppenavn = Bostedsland.navn,
                gruppespørsmålId = Bostedsland.hvilketLandBorDuI.id,
                type = SpørsmålType.LAND,
                svar = LandSvar("NOR"),
            )

        val nesteSpørsmål = Bostedsland.nesteSpørsmål(besvartSpørsmål)

        nesteSpørsmål shouldBe null
    }

    @Test
    fun `neste spørsmål er reistTilbakeTilNorge når hvilketLandBorDuI ikke er Norge`() {
        val besvartSpørsmål =
            Spørsmål(
                spørsmålId = UUID.randomUUID(),
                gruppenavn = Bostedsland.navn,
                gruppespørsmålId = Bostedsland.hvilketLandBorDuI.id,
                type = SpørsmålType.LAND,
                svar = LandSvar("SWE"),
            )

        val nesteSpørsmål = Bostedsland.nesteSpørsmål(besvartSpørsmål)

        nesteSpørsmål shouldBe Bostedsland.reistTilbakeTilNorge
    }

    @Test
    fun `neste spørsmål er datoForAvreise når reistTilbakeTilNorge er true`() {
        val besvartSpørsmål =
            Spørsmål(
                spørsmålId = UUID.randomUUID(),
                gruppenavn = Bostedsland.navn,
                gruppespørsmålId = Bostedsland.reistTilbakeTilNorge.id,
                type = SpørsmålType.BOOLEAN,
                svar = BooleanSvar(true),
            )

        val nesteSpørsmål = Bostedsland.nesteSpørsmål(besvartSpørsmål)

        nesteSpørsmål shouldBe Bostedsland.datoForAvreise
    }

    @Test
    fun `neste spørsmål er enGangIUken når reistTilbakeTilNorge er false`() {
        val besvartSpørsmål =
            Spørsmål(
                spørsmålId = UUID.randomUUID(),
                gruppenavn = Bostedsland.navn,
                gruppespørsmålId = Bostedsland.reistTilbakeTilNorge.id,
                type = SpørsmålType.BOOLEAN,
                svar = BooleanSvar(false),
            )

        val nesteSpørsmål = Bostedsland.nesteSpørsmål(besvartSpørsmål)

        nesteSpørsmål shouldBe Bostedsland.enGangIUken
    }

    @Test
    fun `neste spørsmål er hvorforReisteFraNorge når datoForAvreise er besvart`() {
        val besvartSpørsmål =
            Spørsmål(
                spørsmålId = UUID.randomUUID(),
                gruppenavn = Bostedsland.navn,
                gruppespørsmålId = Bostedsland.datoForAvreise.id,
                type = SpørsmålType.DATO,
                svar = DatoSvar(LocalDate.now()),
            )
        val nesteSpørsmål = Bostedsland.nesteSpørsmål(besvartSpørsmål)

        nesteSpørsmål shouldBe Bostedsland.hvorforReisteFraNorge
    }

    @Test
    fun `neste spørsmål er enGangIUken når hvorforReisteFraNorge er besvart`() {
        val besvartSpørsmål =
            Spørsmål(
                spørsmålId = UUID.randomUUID(),
                gruppenavn = Bostedsland.navn,
                gruppespørsmålId = Bostedsland.hvorforReisteFraNorge.id,
                type = SpørsmålType.TEKST,
                svar = TekstSvar("Derfor"),
            )

        val nesteSpørsmål = Bostedsland.nesteSpørsmål(besvartSpørsmål)

        nesteSpørsmål shouldBe Bostedsland.enGangIUken
    }

    @Test
    fun `neste spørsmål er null når enGangIUken er true`() {
        val besvartSpørsmål =
            Spørsmål(
                spørsmålId = UUID.randomUUID(),
                gruppenavn = Bostedsland.navn,
                gruppespørsmålId = Bostedsland.enGangIUken.id,
                type = SpørsmålType.BOOLEAN,
                svar = BooleanSvar(true),
            )

        val nesteSpørsmål = Bostedsland.nesteSpørsmål(besvartSpørsmål)

        nesteSpørsmål shouldBe null
    }

    @Test
    fun `neste spørsmål er rotasjon når enGangIUken er false`() {
        val besvartSpørsmål =
            Spørsmål(
                spørsmålId = UUID.randomUUID(),
                gruppenavn = Bostedsland.navn,
                gruppespørsmålId = Bostedsland.enGangIUken.id,
                type = SpørsmålType.BOOLEAN,
                svar = BooleanSvar(false),
            )

        val nesteSpørsmål = Bostedsland.nesteSpørsmål(besvartSpørsmål)

        nesteSpørsmål shouldBe Bostedsland.rotasjon
    }

    @Test
    fun `neste spørsmål er null når rotasjon er besvart med true`() {
        val besvartSpørsmål =
            Spørsmål(
                spørsmålId = UUID.randomUUID(),
                gruppenavn = Bostedsland.navn,
                gruppespørsmålId = Bostedsland.rotasjon.id,
                type = SpørsmålType.BOOLEAN,
                svar = BooleanSvar(true),
            )

        val nesteSpørsmål = Bostedsland.nesteSpørsmål(besvartSpørsmål)

        nesteSpørsmål shouldBe null
    }

    @Test
    fun `neste spørsmål er null når rotasjon er besvart med false`() {
        val besvartSpørsmål =
            Spørsmål(
                spørsmålId = UUID.randomUUID(),
                gruppenavn = Bostedsland.navn,
                gruppespørsmålId = Bostedsland.rotasjon.id,
                type = SpørsmålType.BOOLEAN,
                svar = BooleanSvar(false),
            )

        val nesteSpørsmål = Bostedsland.nesteSpørsmål(besvartSpørsmål)

        nesteSpørsmål shouldBe null
    }

    @Test
    fun `neste spørsmål kaster feil dersom svar er null`() {
        val ubesvartSpørsmål =
            Spørsmål(
                spørsmålId = UUID.randomUUID(),
                gruppenavn = Bostedsland.navn,
                gruppespørsmålId = Bostedsland.hvilketLandBorDuI.id,
                type = SpørsmålType.LAND,
                svar = null,
            )

        shouldThrow<IllegalArgumentException> {
            Bostedsland.nesteSpørsmål(ubesvartSpørsmål)
        }
    }

    @Test
    fun `validering kaster ikke feil når svar på hvilketLandBorDuI er gyldig`() {
        val svar = LandSvar(Bostedsland.hvilketLandBorDuI.gyldigeSvar.random())

        shouldNotThrow<IllegalArgumentException> {
            Bostedsland.validerSvar(
                Bostedsland.hvilketLandBorDuI.id,
                svar,
            )
        }
    }

    @Test
    fun `validering kaster feil når svar på hvilketLandBorDuI er ugyldig`() {
        val svar = LandSvar("UGYLDIG")

        shouldThrow<IllegalArgumentException> {
            Bostedsland.validerSvar(Bostedsland.hvilketLandBorDuI.id, svar)
        }
    }

    @Test
    fun `avhengigheter returnerer riktige avhengigheter for hvilketLandBorDuI`() {
        val avhengigheter = Bostedsland.avhengigheter(Bostedsland.hvilketLandBorDuI.id)

        avhengigheter shouldBe
            listOf(
                Bostedsland.reistTilbakeTilNorge.id,
                Bostedsland.datoForAvreise.id,
                Bostedsland.hvorforReisteFraNorge.id,
                Bostedsland.enGangIUken.id,
                Bostedsland.rotasjon.id,
            )
    }

    @Test
    fun `avhengigheter returnerer riktige avhengigheter for reistTilbakeTilNorge`() {
        val avhengigheter = Bostedsland.avhengigheter(Bostedsland.reistTilbakeTilNorge.id)

        avhengigheter shouldBe listOf(Bostedsland.datoForAvreise.id, Bostedsland.hvorforReisteFraNorge.id)
    }

    @Test
    fun `avhengigheter returnerer riktige avhengigheter for datoForAvreise`() {
        val avhengigheter = Bostedsland.avhengigheter(Bostedsland.datoForAvreise.id)

        avhengigheter shouldBe listOf(Bostedsland.hvorforReisteFraNorge.id)
    }

    @Test
    fun `avhengigheter returnerer tom liste for hvorforReisteFraNorge`() {
        val avhengigheter = Bostedsland.avhengigheter(Bostedsland.hvorforReisteFraNorge.id)

        avhengigheter shouldBe emptyList()
    }

    @Test
    fun `avhengigheter returnerer riktige avhengigheter for enGangIUken`() {
        val avhengigheter = Bostedsland.avhengigheter(Bostedsland.enGangIUken.id)

        avhengigheter shouldBe listOf(Bostedsland.rotasjon.id)
    }

    @Test
    fun `avhengigheter returnerer tom liste for rotasjon`() {
        val avhengigheter = Bostedsland.avhengigheter(Bostedsland.rotasjon.id)

        avhengigheter shouldBe emptyList()
    }

    @Test
    fun `avhengigheter kaster IllegalArgumentException for ukjent spørsmålId`() {
        shouldThrow<IllegalArgumentException> {
            Bostedsland.avhengigheter(-1)
        }
    }
}
