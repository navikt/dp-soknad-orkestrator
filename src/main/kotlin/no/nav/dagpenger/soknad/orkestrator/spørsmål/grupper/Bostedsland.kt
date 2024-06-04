package no.nav.dagpenger.soknad.orkestrator.spørsmål.grupper

import no.nav.dagpenger.soknad.orkestrator.spørsmål.BooleanSpørsmål
import no.nav.dagpenger.soknad.orkestrator.spørsmål.LandSpørsmål
import no.nav.dagpenger.soknad.orkestrator.spørsmål.PeriodeSpørsmål
import no.nav.dagpenger.soknad.orkestrator.spørsmål.Spørsmål
import no.nav.dagpenger.soknad.orkestrator.spørsmål.TekstSpørsmål

class Bostedsland {
    internal val navn = Spørsmålgruppe.BOSTEDSLAND

    internal val hvilketLandBorDuI =
        LandSpørsmål(
            tekstnøkkel = "bostedsland.hvilket-land-bor-du-i",
            gyldigeSvar = listOf("NOR", "SWE", "FIN"),
        )

    internal val reistTilbakeTilNorge =
        BooleanSpørsmål(
            tekstnøkkel = "bostedsland.reist-tilbake-til-norge",
        )

    internal val datoForAvreise =
        PeriodeSpørsmål(
            tekstnøkkel = "bostedsland.dato-for-avreise",
        )

    internal val hvorforReisteFraNorge =
        TekstSpørsmål(
            tekstnøkkel = "bostedsland.hvorfor",
        )

    internal val enGangIUken =
        BooleanSpørsmål(
            tekstnøkkel = "bostedsland.en-gang-i-uken",
        )

    internal val rotasjon =
        BooleanSpørsmål(
            tekstnøkkel = "bostedsland.rotasjon",
        )

    fun opprettNesteSpørsmål(aktivtSpørsmål: Spørsmål<*>): Spørsmål<*>? {
        return when (aktivtSpørsmål) {
            hvilketLandBorDuI -> if (aktivtSpørsmål.svar != "NOR") reistTilbakeTilNorge else null
            reistTilbakeTilNorge -> if (aktivtSpørsmål.svar == true) datoForAvreise else enGangIUken
            datoForAvreise -> hvorforReisteFraNorge
            hvorforReisteFraNorge -> enGangIUken
            enGangIUken -> if (aktivtSpørsmål.svar == true) null else rotasjon
            rotasjon -> null
            else -> null
        }
    }
}
