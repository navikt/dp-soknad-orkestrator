package no.nav.dagpenger.soknad.orkestrator.land

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.dagpenger.soknad.orkestrator.api.models.LandDTO
import java.io.FileNotFoundException

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
            val landkode = alpha3Code.uppercase()
            val landnavn = it["no"].asText()

            LandDTO(
                alpha3kode = landkode,
                navn = landnavn,
            )
        }
}
