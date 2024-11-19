package no.nav.dagpenger.soknad.orkestrator.opplysning

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.dagpenger.soknad.orkestrator.opplysning.LandGruppe.ALLE_LAND
import no.nav.dagpenger.soknad.orkestrator.opplysning.LandGruppe.EØS_ELLER_SVEITS
import no.nav.dagpenger.soknad.orkestrator.opplysning.LandGruppe.NORGE
import no.nav.dagpenger.soknad.orkestrator.opplysning.LandGruppe.STORBRITANNIA
import no.nav.dagpenger.soknad.orkestrator.opplysning.LandGruppe.TREDJELAND
import java.io.FileNotFoundException

// TODO: Vil vi ha en enum for hver gruppering av land, eller flere enums som gyldig svar?
enum class LandGruppe {
    ALLE_LAND,
    NORGE,
    STORBRITANNIA,
    EØS_ELLER_SVEITS,
    TREDJELAND,
}

object Landfabrikk {
    val alleLand = LandOppslag.land.toList()
    val norge = alleLand.filter { it.alpha3Code in listOf("NOR", "SJM") }
    val storbritannia = alleLand.filter { it.alpha3Code in listOf("GBR", "JEY", "IMN") }
    val eøsEllerSveitsLandkoder =
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
    val eøsOgSveits = alleLand.filter { it.alpha3Code in eøsEllerSveitsLandkoder }
    val eøsOgSveitsOgStorbritannia = eøsOgSveits + storbritannia
    val tredjeland = alleLand - norge - storbritannia - eøsOgSveits

    fun LandGruppe.hentLandkoder(): List<String> =
        when (this) {
            ALLE_LAND -> alleLand.toLandkoder()
            NORGE -> norge.toLandkoder()
            STORBRITANNIA -> storbritannia.toLandkoder()
            EØS_ELLER_SVEITS -> eøsOgSveits.toLandkoder()
            TREDJELAND -> tredjeland.toLandkoder()
        }

    fun List<Land>.toLandkoder(): List<String> = this.map { it.alpha3Code }
}

internal object LandOppslag {
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

    val land = world.let { mapper.readTree(it) }.map { Land(it["alpha3"].asText().uppercase()) }.toSet()
}

class Land(
    alpha3Code: String,
) : Comparable<Land> {
    val alpha3Code: String

    init {
        require(alpha3Code.length == 3) {
            "ISO 3166-1-alpha3 må være 3 bokstaver lang. Fikk: $alpha3Code"
        }

        this.alpha3Code = alpha3Code.uppercase()
    }

    override fun compareTo(other: Land): Int = this.alpha3Code.compareTo(other.alpha3Code)

    override fun equals(other: Any?): Boolean = other is Land && this.alpha3Code == other.alpha3Code

    override fun hashCode(): Int = this.alpha3Code.hashCode()

    override fun toString(): String = "Land(alpha3Code='$alpha3Code')"
}
