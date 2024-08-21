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
    @Test
    fun `neste spørsmål er null når hvilketLandBorDuI er Norge`() {
        val svar = LandSvar(spørsmålId = UUID.randomUUID(), "NOR")

        val nesteSpørsmål = Bostedsland.nesteSpørsmål(svar, Bostedsland.hvilketLandBorDuI.id)

        nesteSpørsmål shouldBe null
    }

    @Test
    fun `neste spørsmål er reistTilbakeTilNorge når hvilketLandBorDuI ikke er Norge`() {
        val svar = LandSvar(spørsmålId = UUID.randomUUID(), "SWE")

        val nesteSpørsmål = Bostedsland.nesteSpørsmål(svar, Bostedsland.hvilketLandBorDuI.id)

        nesteSpørsmål shouldBe Bostedsland.reistTilbakeTilNorge
    }

    @Test
    fun `neste spørsmål er datoForAvreise når reistTilbakeTilNorge er true`() {
        val svar = BooleanSvar(spørsmålId = UUID.randomUUID(), true)

        val nesteSpørsmål = Bostedsland.nesteSpørsmål(svar, Bostedsland.reistTilbakeTilNorge.id)

        nesteSpørsmål shouldBe Bostedsland.datoForAvreise
    }

    @Test
    fun `neste spørsmål er enGangIUken når reistTilbakeTilNorge er false`() {
        val svar = BooleanSvar(spørsmålId = UUID.randomUUID(), false)

        val nesteSpørsmål = Bostedsland.nesteSpørsmål(svar, Bostedsland.reistTilbakeTilNorge.id)

        nesteSpørsmål shouldBe Bostedsland.enGangIUken
    }

    @Test
    fun `neste spørsmål er hvorforReisteFraNorge når datoForAvreise er besvart`() {
        val svar = DatoSvar(spørsmålId = UUID.randomUUID(), LocalDate.now())

        val nesteSpørsmål = Bostedsland.nesteSpørsmål(svar, Bostedsland.datoForAvreise.id)

        nesteSpørsmål shouldBe Bostedsland.hvorforReisteFraNorge
    }

    @Test
    fun `neste spørsmål er enGangIUken når hvorforReisteFraNorge er besvart`() {
        val svar = TekstSvar(spørsmålId = UUID.randomUUID(), "Derfor")

        val nesteSpørsmål = Bostedsland.nesteSpørsmål(svar, Bostedsland.hvorforReisteFraNorge.id)

        nesteSpørsmål shouldBe Bostedsland.enGangIUken
    }

    @Test
    fun `neste spørsmål er null når enGangIUken er true`() {
        val svar = BooleanSvar(spørsmålId = UUID.randomUUID(), true)

        val nesteSpørsmål = Bostedsland.nesteSpørsmål(svar, Bostedsland.enGangIUken.id)

        nesteSpørsmål shouldBe null
    }

    @Test
    fun `neste spørsmål er rotasjon når enGangIUken er false`() {
        val svar = BooleanSvar(spørsmålId = UUID.randomUUID(), false)

        val nesteSpørsmål = Bostedsland.nesteSpørsmål(svar, Bostedsland.enGangIUken.id)

        nesteSpørsmål shouldBe Bostedsland.rotasjon
    }

    @Test
    fun `neste spørsmål er null når rotasjon er besvart med true`() {
        val svar = BooleanSvar(spørsmålId = UUID.randomUUID(), true)

        val nesteSpørsmål = Bostedsland.nesteSpørsmål(svar, Bostedsland.rotasjon.id)

        nesteSpørsmål shouldBe null
    }

    @Test
    fun `neste spørsmål er null når rotasjon er besvart med false`() {
        val svar = BooleanSvar(spørsmålId = UUID.randomUUID(), false)

        val nesteSpørsmål = Bostedsland.nesteSpørsmål(svar, Bostedsland.rotasjon.id)

        nesteSpørsmål shouldBe null
    }

    @Test
    fun `validering kaster ikke feil når svar på hvilketLandBorDuI er gyldig`() {
        val svar =
            LandSvar(
                spørsmålId = UUID.randomUUID(),
                verdi = Bostedsland.hvilketLandBorDuI.gyldigeSvar.random(),
            )

        shouldNotThrow<IllegalArgumentException> {
            Bostedsland.validerSvar(
                Bostedsland.hvilketLandBorDuI.id,
                svar,
            )
        }
    }

    @Test
    fun `validering kaster feil når svar på hvilketLandBorDuI ikke er definert i gyldigeValg`() {
        val svar =
            LandSvar(
                spørsmålId = UUID.randomUUID(),
                verdi = "XXX",
            )

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
