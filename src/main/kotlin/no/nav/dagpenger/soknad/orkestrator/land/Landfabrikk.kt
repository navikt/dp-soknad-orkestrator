package no.nav.dagpenger.soknad.orkestrator.land

object Landfabrikk {
    val alleLand = Landoppslag.land.toList()
    private val norge = alleLand.filter { it.alpha3kode in listOf("NOR", "SJM") }
    private val storbritannia = alleLand.filter { it.alpha3kode in listOf("GBR", "JEY", "IMN") }
    private val eøsOgSveitsLandkoder =
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
    private val eøsOgSveits = alleLand.filter { it.alpha3kode in eøsOgSveitsLandkoder }
    private val eøsOgSveitsOgStorbritannia = eøsOgSveits + storbritannia
    private val tredjeland = alleLand - norge - storbritannia - eøsOgSveits

    fun Landgruppe.landkoder(): List<String> =
        when (this) {
            Landgruppe.NORGE -> norge.map { it.alpha3kode }
            Landgruppe.STORBRITANNIA -> storbritannia.map { it.alpha3kode }
            Landgruppe.EØS_OG_SVEITS -> eøsOgSveits.map { it.alpha3kode }
            Landgruppe.TREDJELAND -> tredjeland.map { it.alpha3kode }
        }
}
