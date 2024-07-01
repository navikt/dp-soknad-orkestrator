package no.nav.dagpenger.soknad.orkestrator.spørsmål.grupper

import no.nav.dagpenger.soknad.orkestrator.api.models.SporsmaalgruppeNavnDTO
import no.nav.dagpenger.soknad.orkestrator.api.models.SporsmalDTO
import no.nav.dagpenger.soknad.orkestrator.api.models.SporsmalTypeDTO
import no.nav.dagpenger.soknad.orkestrator.api.models.SporsmalgruppeDTO
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.PeriodeSvar
import java.time.LocalDate
import java.util.UUID

val mock1: SporsmalgruppeDTO =
    SporsmalgruppeDTO(
        id = 1,
        navn = SporsmaalgruppeNavnDTO.BOSTEDSLAND,
        nesteSpørsmål =
            SporsmalDTO(
                id = UUID.randomUUID(),
                tekstnøkkel = "bostedsland.hvilket-land-bor-du-i",
                type = SporsmalTypeDTO.LAND,
                gyldigeSvar = listOf("NOR", "SWE", "FIN", "NLD"),
            ),
        besvarteSpørsmål = emptyList(),
    )

val mock2: SporsmalgruppeDTO =
    SporsmalgruppeDTO(
        id = 1,
        navn = SporsmaalgruppeNavnDTO.BOSTEDSLAND,
        nesteSpørsmål =
            SporsmalDTO(
                id = UUID.randomUUID(),
                tekstnøkkel = "bostedsland.reist-tilbake-til-norge",
                type = SporsmalTypeDTO.BOOLEAN,
            ),
        besvarteSpørsmål =
            listOf(
                SporsmalDTO(
                    id = UUID.randomUUID(),
                    tekstnøkkel = "bostedsland.hvilket-land-bor-du-i",
                    type = SporsmalTypeDTO.LAND,
                    svar = "NLD",
                    gyldigeSvar = listOf("NOR", "SWE", "FIN", "NLD"),
                ),
            ),
    )

val mock3: SporsmalgruppeDTO =
    SporsmalgruppeDTO(
        id = 1,
        navn = SporsmaalgruppeNavnDTO.BOSTEDSLAND,
        nesteSpørsmål =
            SporsmalDTO(
                id = UUID.randomUUID(),
                tekstnøkkel = "bostedsland.dato-for-avreise",
                type = SporsmalTypeDTO.PERIODE,
            ),
        besvarteSpørsmål =
            listOf(
                SporsmalDTO(
                    id = UUID.randomUUID(),
                    tekstnøkkel = "bostedsland.hvilket-land-bor-du-i",
                    type = SporsmalTypeDTO.LAND,
                    svar = "NLD",
                    gyldigeSvar = listOf("NOR", "SWE", "FIN", "NLD"),
                ),
                SporsmalDTO(
                    id = UUID.randomUUID(),
                    tekstnøkkel = "bostedsland.reist-tilbake-til-norge",
                    type = SporsmalTypeDTO.BOOLEAN,
                    svar = "true",
                ),
            ),
    )

val mock4: SporsmalgruppeDTO =
    SporsmalgruppeDTO(
        id = 1,
        navn = SporsmaalgruppeNavnDTO.BOSTEDSLAND,
        nesteSpørsmål =
            SporsmalDTO(
                id = UUID.randomUUID(),
                tekstnøkkel = "bostedsland.hvorfor",
                type = SporsmalTypeDTO.TEKST,
            ),
        besvarteSpørsmål =
            listOf(
                SporsmalDTO(
                    id = UUID.randomUUID(),
                    tekstnøkkel = "bostedsland.hvilket-land-bor-du-i",
                    type = SporsmalTypeDTO.LAND,
                    svar = "NLD",
                    gyldigeSvar = listOf("NOR", "SWE", "FIN", "NLD"),
                ),
                SporsmalDTO(
                    id = UUID.randomUUID(),
                    tekstnøkkel = "bostedsland.reist-tilbake-til-norge",
                    type = SporsmalTypeDTO.BOOLEAN,
                    svar = "true",
                ),
                SporsmalDTO(
                    id = UUID.randomUUID(),
                    tekstnøkkel = "bostedsland.dato-for-avreise",
                    type = SporsmalTypeDTO.PERIODE,
                    svar = PeriodeSvar(LocalDate.now().minusDays(14), LocalDate.now()).toString(),
                ),
            ),
    )

val mock5: SporsmalgruppeDTO =
    SporsmalgruppeDTO(
        id = 1,
        navn = SporsmaalgruppeNavnDTO.BOSTEDSLAND,
        nesteSpørsmål =
            SporsmalDTO(
                id = UUID.randomUUID(),
                tekstnøkkel = "bostedsland.en-gang-i-uken",
                type = SporsmalTypeDTO.BOOLEAN,
            ),
        besvarteSpørsmål =
            listOf(
                SporsmalDTO(
                    id = UUID.randomUUID(),
                    tekstnøkkel = "bostedsland.hvilket-land-bor-du-i",
                    type = SporsmalTypeDTO.LAND,
                    svar = "NLD",
                    gyldigeSvar = listOf("NOR", "SWE", "FIN", "NLD"),
                ),
                SporsmalDTO(
                    id = UUID.randomUUID(),
                    tekstnøkkel = "bostedsland.reist-tilbake-til-norge",
                    type = SporsmalTypeDTO.BOOLEAN,
                    svar = "true",
                ),
                SporsmalDTO(
                    id = UUID.randomUUID(),
                    tekstnøkkel = "bostedsland.dato-for-avreise",
                    type = SporsmalTypeDTO.PERIODE,
                    svar = PeriodeSvar(LocalDate.now().minusDays(14), LocalDate.now()).toString(),
                ),
                SporsmalDTO(
                    id = UUID.randomUUID(),
                    tekstnøkkel = "bostedsland.hvorfor",
                    type = SporsmalTypeDTO.TEKST,
                    svar = "Derfor",
                ),
            ),
    )

val mock6: SporsmalgruppeDTO =
    SporsmalgruppeDTO(
        id = 1,
        navn = SporsmaalgruppeNavnDTO.BOSTEDSLAND,
        nesteSpørsmål =
            SporsmalDTO(
                id = UUID.randomUUID(),
                tekstnøkkel = "bostedsland.rotasjon",
                type = SporsmalTypeDTO.BOOLEAN,
            ),
        besvarteSpørsmål =
            listOf(
                SporsmalDTO(
                    id = UUID.randomUUID(),
                    tekstnøkkel = "bostedsland.hvilket-land-bor-du-i",
                    type = SporsmalTypeDTO.LAND,
                    svar = "NLD",
                    gyldigeSvar = listOf("NOR", "SWE", "FIN", "NLD"),
                ),
                SporsmalDTO(
                    id = UUID.randomUUID(),
                    tekstnøkkel = "bostedsland.reist-tilbake-til-norge",
                    type = SporsmalTypeDTO.BOOLEAN,
                    svar = "true",
                ),
                SporsmalDTO(
                    id = UUID.randomUUID(),
                    tekstnøkkel = "bostedsland.dato-for-avreise",
                    type = SporsmalTypeDTO.PERIODE,
                    svar = PeriodeSvar(LocalDate.now().minusDays(14), LocalDate.now()).toString(),
                ),
                SporsmalDTO(
                    id = UUID.randomUUID(),
                    tekstnøkkel = "bostedsland.hvorfor",
                    type = SporsmalTypeDTO.TEKST,
                    svar = "Derfor",
                ),
                SporsmalDTO(
                    id = UUID.randomUUID(),
                    tekstnøkkel = "bostedsland.en-gang-i-uken",
                    type = SporsmalTypeDTO.BOOLEAN,
                    svar = "true",
                ),
            ),
    )

val mock7: SporsmalgruppeDTO =
    SporsmalgruppeDTO(
        id = 1,
        navn = SporsmaalgruppeNavnDTO.BOSTEDSLAND,
        nesteSpørsmål = null,
        besvarteSpørsmål =
            listOf(
                SporsmalDTO(
                    id = UUID.randomUUID(),
                    tekstnøkkel = "bostedsland.hvilket-land-bor-du-i",
                    type = SporsmalTypeDTO.LAND,
                    svar = "NLD",
                    gyldigeSvar = listOf("NOR", "SWE", "FIN", "NLD"),
                ),
                SporsmalDTO(
                    id = UUID.randomUUID(),
                    tekstnøkkel = "bostedsland.reist-tilbake-til-norge",
                    type = SporsmalTypeDTO.BOOLEAN,
                    svar = "true",
                ),
                SporsmalDTO(
                    id = UUID.randomUUID(),
                    tekstnøkkel = "bostedsland.dato-for-avreise",
                    type = SporsmalTypeDTO.PERIODE,
                    svar = PeriodeSvar(LocalDate.now().minusDays(14), LocalDate.now()).toString(),
                ),
                SporsmalDTO(
                    id = UUID.randomUUID(),
                    tekstnøkkel = "bostedsland.hvorfor",
                    type = SporsmalTypeDTO.TEKST,
                    svar = "Derfor",
                ),
                SporsmalDTO(
                    id = UUID.randomUUID(),
                    tekstnøkkel = "bostedsland.en-gang-i-uken",
                    type = SporsmalTypeDTO.BOOLEAN,
                    svar = "true",
                ),
                SporsmalDTO(
                    id = UUID.randomUUID(),
                    tekstnøkkel = "bostedsland.rotasjon",
                    type = SporsmalTypeDTO.BOOLEAN,
                    svar = "false",
                ),
            ),
    )

val mockSpørsmålgrupper: List<SporsmalgruppeDTO> = listOf(mock1, mock2, mock3, mock4, mock5, mock6, mock7)
