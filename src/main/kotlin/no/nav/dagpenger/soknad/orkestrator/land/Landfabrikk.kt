package no.nav.dagpenger.soknad.orkestrator.land

import no.nav.dagpenger.soknad.orkestrator.api.models.LandgruppeDTO

object Landfabrikk {
    val alleLand = Landoppslag.land.toList()
    val norge = alleLand.filter { it in listOf("NOR", "SJM") }
    val storbritannia = alleLand.filter { it in listOf("GBR", "JEY", "IMN") }
    val eøsOgSveitsLandkoder =
        listOf(
            "BEL",
            "BGR",
            "DNK",
            "EST",
            "FIN",
            "FRA",
            "GRC",
            "IRL",
            "ISL",
            "ITA",
            "HRV",
            "CYP",
            "LVA",
            "LIE",
            "LTU",
            "LUX",
            "MLT",
            "NLD",
            "POL",
            "PRT",
            "ROU",
            "SVK",
            "SVN",
            "ESP",
            "CHE",
            "SWE",
            "CZE",
            "DEU",
            "HUN",
            "AUT",
        )
    val eøsOgSveits = alleLand.filter { it in eøsOgSveitsLandkoder }
    val eøsOgSveitsOgStorbritannia = eøsOgSveits + storbritannia
    val tredjeland = alleLand - norge - storbritannia - eøsOgSveits

    fun Landgruppe.hentLandkoder(): List<String> =
        when (this) {
            Landgruppe.NORGE -> norge
            Landgruppe.STORBRITANNIA -> storbritannia
            Landgruppe.EØS_OG_SVEITS -> eøsOgSveits
            Landgruppe.TREDJELAND -> tredjeland
        }

    fun alleLandgrupper(): List<LandgruppeDTO> =
        Landgruppe.entries.map {
            LandgruppeDTO(
                gruppenavn = LandgruppeDTO.Gruppenavn.valueOf(it.name),
                land = it.hentLandkoder(),
                gruppeId = "gruppe.${it.name.lowercase()}",
            )
        }
}
