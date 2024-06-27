package no.nav.dagpenger.soknad.orkestrator.spørsmål.grupper

import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.PeriodeSvar
import no.nav.dagpenger.soknad.orkestrator.spørsmål.SpørsmålDTO
import no.nav.dagpenger.soknad.orkestrator.spørsmål.SpørsmålType
import java.time.LocalDate
import java.util.UUID

val mock1: SpørsmålgruppeDTO =
    SpørsmålgruppeDTO(
        id = 1,
        navn = Spørsmålgruppe.BOSTEDSLAND,
        nesteSpørsmål =
            SpørsmålDTO<String>(
                id = UUID.randomUUID(),
                tekstnøkkel = "bostedsland.hvilket-land-bor-du-i",
                type = SpørsmålType.LAND,
                gyldigeSvar = listOf("NOR", "SWE", "FIN", "NLD"),
            ),
        besvarteSpørsmål = emptyList(),
    )

val mock2: SpørsmålgruppeDTO =
    SpørsmålgruppeDTO(
        id = 1,
        navn = Spørsmålgruppe.BOSTEDSLAND,
        nesteSpørsmål =
            SpørsmålDTO<Boolean>(
                id = UUID.randomUUID(),
                tekstnøkkel = "bostedsland.reist-tilbake-til-norge",
                type = SpørsmålType.BOOLEAN,
            ),
        besvarteSpørsmål =
            listOf(
                SpørsmålDTO(
                    id = UUID.randomUUID(),
                    tekstnøkkel = "bostedsland.hvilket-land-bor-du-i",
                    type = SpørsmålType.LAND,
                    svar = "NLD",
                    gyldigeSvar = listOf("NOR", "SWE", "FIN", "NLD"),
                ),
            ),
    )

val mock3: SpørsmålgruppeDTO =
    SpørsmålgruppeDTO(
        id = 1,
        navn = Spørsmålgruppe.BOSTEDSLAND,
        nesteSpørsmål =
            SpørsmålDTO<PeriodeSvar>(
                id = UUID.randomUUID(),
                tekstnøkkel = "bostedsland.dato-for-avreise",
                type = SpørsmålType.PERIODE,
            ),
        besvarteSpørsmål =
            listOf(
                SpørsmålDTO(
                    id = UUID.randomUUID(),
                    tekstnøkkel = "bostedsland.hvilket-land-bor-du-i",
                    type = SpørsmålType.LAND,
                    svar = "NLD",
                    gyldigeSvar = listOf("NOR", "SWE", "FIN", "NLD"),
                ),
                SpørsmålDTO(
                    id = UUID.randomUUID(),
                    tekstnøkkel = "bostedsland.reist-tilbake-til-norge",
                    type = SpørsmålType.BOOLEAN,
                    svar = true,
                ),
            ),
    )

val mock4: SpørsmålgruppeDTO =
    SpørsmålgruppeDTO(
        id = 1,
        navn = Spørsmålgruppe.BOSTEDSLAND,
        nesteSpørsmål =
            SpørsmålDTO<String>(
                id = UUID.randomUUID(),
                tekstnøkkel = "bostedsland.hvorfor",
                type = SpørsmålType.TEKST,
            ),
        besvarteSpørsmål =
            listOf(
                SpørsmålDTO(
                    id = UUID.randomUUID(),
                    tekstnøkkel = "bostedsland.hvilket-land-bor-du-i",
                    type = SpørsmålType.LAND,
                    svar = "NLD",
                    gyldigeSvar = listOf("NOR", "SWE", "FIN", "NLD"),
                ),
                SpørsmålDTO(
                    id = UUID.randomUUID(),
                    tekstnøkkel = "bostedsland.reist-tilbake-til-norge",
                    type = SpørsmålType.BOOLEAN,
                    svar = true,
                ),
                SpørsmålDTO(
                    id = UUID.randomUUID(),
                    tekstnøkkel = "bostedsland.dato-for-avreise",
                    type = SpørsmålType.PERIODE,
                    svar = PeriodeSvar(LocalDate.now().minusDays(14), LocalDate.now()),
                ),
            ),
    )

val mock5: SpørsmålgruppeDTO =
    SpørsmålgruppeDTO(
        id = 1,
        navn = Spørsmålgruppe.BOSTEDSLAND,
        nesteSpørsmål =
            SpørsmålDTO<Boolean>(
                id = UUID.randomUUID(),
                tekstnøkkel = "bostedsland.en-gang-i-uken",
                type = SpørsmålType.BOOLEAN,
            ),
        besvarteSpørsmål =
            listOf(
                SpørsmålDTO(
                    id = UUID.randomUUID(),
                    tekstnøkkel = "bostedsland.hvilket-land-bor-du-i",
                    type = SpørsmålType.LAND,
                    svar = "NLD",
                    gyldigeSvar = listOf("NOR", "SWE", "FIN", "NLD"),
                ),
                SpørsmålDTO(
                    id = UUID.randomUUID(),
                    tekstnøkkel = "bostedsland.reist-tilbake-til-norge",
                    type = SpørsmålType.BOOLEAN,
                    svar = true,
                ),
                SpørsmålDTO(
                    id = UUID.randomUUID(),
                    tekstnøkkel = "bostedsland.dato-for-avreise",
                    type = SpørsmålType.PERIODE,
                    svar = PeriodeSvar(LocalDate.now().minusDays(14), LocalDate.now()),
                ),
                SpørsmålDTO(
                    id = UUID.randomUUID(),
                    tekstnøkkel = "bostedsland.hvorfor",
                    type = SpørsmålType.TEKST,
                    svar = "Derfor",
                ),
            ),
    )

val mock6: SpørsmålgruppeDTO =
    SpørsmålgruppeDTO(
        id = 1,
        navn = Spørsmålgruppe.BOSTEDSLAND,
        nesteSpørsmål =
            SpørsmålDTO<Boolean>(
                id = UUID.randomUUID(),
                tekstnøkkel = "bostedsland.rotasjon",
                type = SpørsmålType.BOOLEAN,
            ),
        besvarteSpørsmål =
            listOf(
                SpørsmålDTO(
                    id = UUID.randomUUID(),
                    tekstnøkkel = "bostedsland.hvilket-land-bor-du-i",
                    type = SpørsmålType.LAND,
                    svar = "NLD",
                    gyldigeSvar = listOf("NOR", "SWE", "FIN", "NLD"),
                ),
                SpørsmålDTO(
                    id = UUID.randomUUID(),
                    tekstnøkkel = "bostedsland.reist-tilbake-til-norge",
                    type = SpørsmålType.BOOLEAN,
                    svar = true,
                ),
                SpørsmålDTO(
                    id = UUID.randomUUID(),
                    tekstnøkkel = "bostedsland.dato-for-avreise",
                    type = SpørsmålType.PERIODE,
                    svar = PeriodeSvar(LocalDate.now().minusDays(14), LocalDate.now()),
                ),
                SpørsmålDTO(
                    id = UUID.randomUUID(),
                    tekstnøkkel = "bostedsland.hvorfor",
                    type = SpørsmålType.TEKST,
                    svar = "Derfor",
                ),
                SpørsmålDTO(
                    id = UUID.randomUUID(),
                    tekstnøkkel = "bostedsland.en-gang-i-uken",
                    type = SpørsmålType.BOOLEAN,
                    svar = true,
                ),
            ),
    )

val mock7: SpørsmålgruppeDTO =
    SpørsmålgruppeDTO(
        id = 1,
        navn = Spørsmålgruppe.BOSTEDSLAND,
        nesteSpørsmål = null,
        besvarteSpørsmål =
            listOf(
                SpørsmålDTO(
                    id = UUID.randomUUID(),
                    tekstnøkkel = "bostedsland.hvilket-land-bor-du-i",
                    type = SpørsmålType.LAND,
                    svar = "NLD",
                    gyldigeSvar = listOf("NOR", "SWE", "FIN", "NLD"),
                ),
                SpørsmålDTO(
                    id = UUID.randomUUID(),
                    tekstnøkkel = "bostedsland.reist-tilbake-til-norge",
                    type = SpørsmålType.BOOLEAN,
                    svar = true,
                ),
                SpørsmålDTO(
                    id = UUID.randomUUID(),
                    tekstnøkkel = "bostedsland.dato-for-avreise",
                    type = SpørsmålType.PERIODE,
                    svar = PeriodeSvar(LocalDate.now().minusDays(14), LocalDate.now()),
                ),
                SpørsmålDTO(
                    id = UUID.randomUUID(),
                    tekstnøkkel = "bostedsland.hvorfor",
                    type = SpørsmålType.TEKST,
                    svar = "Derfor",
                ),
                SpørsmålDTO(
                    id = UUID.randomUUID(),
                    tekstnøkkel = "bostedsland.en-gang-i-uken",
                    type = SpørsmålType.BOOLEAN,
                    svar = true,
                ),
                SpørsmålDTO(
                    id = UUID.randomUUID(),
                    tekstnøkkel = "bostedsland.rotasjon",
                    type = SpørsmålType.BOOLEAN,
                    svar = false,
                ),
            ),
    )

val mockSpørsmålgrupper: List<SpørsmålgruppeDTO> = listOf(mock1, mock2, mock3, mock4, mock5, mock6, mock7)
