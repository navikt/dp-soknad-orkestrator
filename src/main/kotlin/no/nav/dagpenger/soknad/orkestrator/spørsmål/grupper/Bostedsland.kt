package no.nav.dagpenger.soknad.orkestrator.spørsmål.grupper

import no.nav.dagpenger.soknad.orkestrator.api.models.SporsmalDTO
import no.nav.dagpenger.soknad.orkestrator.api.models.SporsmalTypeDTO
import no.nav.dagpenger.soknad.orkestrator.spørsmål.BooleanSpørsmål
import no.nav.dagpenger.soknad.orkestrator.spørsmål.LandSpørsmål
import no.nav.dagpenger.soknad.orkestrator.spørsmål.PeriodeSpørsmål
import no.nav.dagpenger.soknad.orkestrator.spørsmål.Spørsmål
import no.nav.dagpenger.soknad.orkestrator.spørsmål.TekstSpørsmål
import java.util.UUID

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

class BostedslandDTO {
    internal val navn = Spørsmålgruppe.BOSTEDSLAND

    val hvilketLandBorDuI =
        SporsmalDTO(
            id = UUID.randomUUID(),
            tekstnøkkel = "bostedsland.hvilket-land-bor-du-i",
            type = SporsmalTypeDTO.LAND,
            gyldigeSvar = listOf("NOR", "SWE", "FIN"),
        )

    val reistTilbakeTilNorge =
        SporsmalDTO(
            id = UUID.randomUUID(),
            tekstnøkkel = "bostedsland.reist-tilbake-til-norge",
            type = SporsmalTypeDTO.BOOLEAN,
        )

    val datoForAvreise =
        SporsmalDTO(
            id = UUID.randomUUID(),
            tekstnøkkel = "bostedsland.dato-for-avreise",
            type = SporsmalTypeDTO.PERIODE,
        )

    val hvorforReisteFraNorge =
        SporsmalDTO(
            id = UUID.randomUUID(),
            tekstnøkkel = "bostedsland.hvorfor",
            type = SporsmalTypeDTO.TEKST,
        )

    val enGangIUken =
        SporsmalDTO(
            id = UUID.randomUUID(),
            tekstnøkkel = "bostedsland.en-gang-i-uken",
            type = SporsmalTypeDTO.BOOLEAN,
        )

    val rotasjon =
        SporsmalDTO(
            id = UUID.randomUUID(),
            tekstnøkkel = "bostedsland.rotasjon",
            type = SporsmalTypeDTO.BOOLEAN,
        )

    fun nesteSpørsmål(aktivtSpørsmål: SporsmalDTO): SporsmalDTO? {
        return when (aktivtSpørsmål.tekstnøkkel) {
            hvilketLandBorDuI.tekstnøkkel -> if (aktivtSpørsmål.svar != "NOR") reistTilbakeTilNorge else null
            reistTilbakeTilNorge.tekstnøkkel -> if (aktivtSpørsmål.svar == "true") datoForAvreise else enGangIUken
            datoForAvreise.tekstnøkkel -> hvorforReisteFraNorge
            hvorforReisteFraNorge.tekstnøkkel -> enGangIUken
            enGangIUken.tekstnøkkel -> if (aktivtSpørsmål.svar == "true") null else rotasjon
            rotasjon.tekstnøkkel -> null
            else -> null
        }
    }
}
