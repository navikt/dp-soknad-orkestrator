package no.nav.dagpenger.soknad.orkestrator.opplysning.seksjoner

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.soknad.orkestrator.opplysning.BooleanSvar
import no.nav.dagpenger.soknad.orkestrator.opplysning.DatoSvar
import no.nav.dagpenger.soknad.orkestrator.opplysning.LandSvar
import no.nav.dagpenger.soknad.orkestrator.opplysning.Landfabrikk
import no.nav.dagpenger.soknad.orkestrator.opplysning.Landfabrikk.eøsOgSveits
import no.nav.dagpenger.soknad.orkestrator.opplysning.TekstSvar
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test

class BostedslandTest {
    @Test
    fun `neste opplysning er null når hvilketLandBorDuI er Norge`() {
        val svar = LandSvar(opplysningId = UUID.randomUUID(), "NOR")

        val nesteOpplysningsbehov = Bostedsland.nesteOpplysningsbehov(svar, Bostedsland.hvilketLandBorDuI.id)

        nesteOpplysningsbehov shouldBe null
    }

    @Test
    fun `neste opplysning er null når hvilketLandBorDuI er et tredjeland`() {
        val svar = LandSvar(opplysningId = UUID.randomUUID(), Landfabrikk.tredjeland.random().alpha3Code)

        val nesteOpplysningsbehov = Bostedsland.nesteOpplysningsbehov(svar, Bostedsland.hvilketLandBorDuI.id)

        nesteOpplysningsbehov shouldBe null
    }

    @Test
    fun `neste opplysning er reistTilbakeTilNorge når hvilketLandBorDuI er et EØS-land eller Sveits`() {
        val eøsLandSvar = LandSvar(opplysningId = UUID.randomUUID(), eøsOgSveits.random().alpha3Code)

        val nesteOpplysningsbehov = Bostedsland.nesteOpplysningsbehov(eøsLandSvar, Bostedsland.hvilketLandBorDuI.id)

        nesteOpplysningsbehov shouldBe Bostedsland.reistTilbakeTilNorge
    }

    @Test
    fun `neste opplysning er datoForAvreise når reistTilbakeTilNorge er true`() {
        val svar = BooleanSvar(opplysningId = UUID.randomUUID(), true)

        val nesteOpplysningsbehov = Bostedsland.nesteOpplysningsbehov(svar, Bostedsland.reistTilbakeTilNorge.id)

        nesteOpplysningsbehov shouldBe Bostedsland.datoForAvreise
    }

    @Test
    fun `neste opplysning er enGangIUken når reistTilbakeTilNorge er false`() {
        val svar = BooleanSvar(opplysningId = UUID.randomUUID(), false)

        val nesteOpplysningsbehov = Bostedsland.nesteOpplysningsbehov(svar, Bostedsland.reistTilbakeTilNorge.id)

        nesteOpplysningsbehov shouldBe Bostedsland.enGangIUken
    }

    @Test
    fun `neste opplysning er hvorforReisteFraNorge når datoForAvreise er besvart`() {
        val svar = DatoSvar(opplysningId = UUID.randomUUID(), LocalDate.now())

        val nesteOpplysningsbehov = Bostedsland.nesteOpplysningsbehov(svar, Bostedsland.datoForAvreise.id)

        nesteOpplysningsbehov shouldBe Bostedsland.hvorforReisteFraNorge
    }

    @Test
    fun `neste opplysning er enGangIUken når hvorforReisteFraNorge er besvart`() {
        val svar = TekstSvar(opplysningId = UUID.randomUUID(), "Derfor")

        val nesteOpplysningsbehov = Bostedsland.nesteOpplysningsbehov(svar, Bostedsland.hvorforReisteFraNorge.id)

        nesteOpplysningsbehov shouldBe Bostedsland.enGangIUken
    }

    @Test
    fun `neste opplysning er null når enGangIUken er true`() {
        val svar = BooleanSvar(opplysningId = UUID.randomUUID(), true)

        val nesteOpplysningsbehov = Bostedsland.nesteOpplysningsbehov(svar, Bostedsland.enGangIUken.id)

        nesteOpplysningsbehov shouldBe null
    }

    @Test
    fun `neste opplysning er rotasjon når enGangIUken er false`() {
        val svar = BooleanSvar(opplysningId = UUID.randomUUID(), false)

        val nesteOpplysningsbehov = Bostedsland.nesteOpplysningsbehov(svar, Bostedsland.enGangIUken.id)

        nesteOpplysningsbehov shouldBe Bostedsland.rotasjon
    }

    @Test
    fun `neste opplysing er null når rotasjon er besvart med true`() {
        val svar = BooleanSvar(opplysningId = UUID.randomUUID(), true)

        val nesteOpplysningsbehov = Bostedsland.nesteOpplysningsbehov(svar, Bostedsland.rotasjon.id)

        nesteOpplysningsbehov shouldBe null
    }

    @Test
    fun `neste opplysing er null når rotasjon er besvart med false`() {
        val svar = BooleanSvar(opplysningId = UUID.randomUUID(), false)

        val nesteOpplysningsbehov = Bostedsland.nesteOpplysningsbehov(svar, Bostedsland.rotasjon.id)

        nesteOpplysningsbehov shouldBe null
    }

    @Test
    fun `validering kaster ikke feil når svar på hvilketLandBorDuI er gyldig`() {
        val svar =
            LandSvar(
                opplysningId = UUID.randomUUID(),
                verdi = "SWE",
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
                opplysningId = UUID.randomUUID(),
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
    fun `avhengigheter kaster IllegalArgumentException for ukjent opplysingId`() {
        shouldThrow<IllegalArgumentException> {
            Bostedsland.avhengigheter(-1)
        }
    }
}
