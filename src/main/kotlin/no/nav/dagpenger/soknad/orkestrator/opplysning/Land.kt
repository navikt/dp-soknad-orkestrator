package no.nav.dagpenger.soknad.orkestrator.opplysning

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.dagpenger.soknad.orkestrator.api.models.LandgruppeDTO
import no.nav.dagpenger.soknad.orkestrator.opplysning.Landgruppe.EØS_OG_SVEITS
import no.nav.dagpenger.soknad.orkestrator.opplysning.Landgruppe.NORGE
import no.nav.dagpenger.soknad.orkestrator.opplysning.Landgruppe.STORBRITANNIA
import no.nav.dagpenger.soknad.orkestrator.opplysning.Landgruppe.TREDJELAND
import java.io.FileNotFoundException

enum class Landgruppe {
    NORGE,
    STORBRITANNIA,
    EØS_OG_SVEITS,
    TREDJELAND,
}

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
            NORGE -> norge
            STORBRITANNIA -> storbritannia
            EØS_OG_SVEITS -> eøsOgSveits
            TREDJELAND -> tredjeland
        }

    fun tilLandgruppeDTO(landgrupper: List<Landgruppe>): List<LandgruppeDTO> =
        landgrupper.map {
            LandgruppeDTO(
                land = it.hentLandkoder(),
                gruppeId = "gruppe.${it.name.lowercase()}",
            )
        }
}

internal object Landoppslag {
    /**
     * world.json hentet fra https://github.com/stefangabos/world_countries
     */
    private const val WORLD_JSON = "/world.json"
    private val mapper = jacksonObjectMapper()

    private val world by lazy {
        this.javaClass.getResource(WORLD_JSON)?.openStream()?.buffered()?.reader()?.use {
            it.readText()
        } ?: throw FileNotFoundException("Fant ikke filen $WORLD_JSON")
    }

    val land =
        world.let { mapper.readTree(it) }.map {
            val alpha3Code = it["alpha3"].asText()
            require(alpha3Code.length == 3) {
                "ISO 3166-1-alpha3 må være 3 bokstaver lang. Fikk: $alpha3Code"
            }
            alpha3Code.uppercase()
        }.toSet()
}
