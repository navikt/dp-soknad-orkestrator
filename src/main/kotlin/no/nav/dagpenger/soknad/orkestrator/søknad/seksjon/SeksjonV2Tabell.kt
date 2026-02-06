package no.nav.dagpenger.soknad.orkestrator.søknad.seksjon

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadTabell
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.json.json
import java.time.LocalDateTime
import java.util.UUID

object SeksjonV2Tabell : IntIdTable("seksjon_v2") {
    val seksjonId: Column<String> = text("seksjon_id")
    val søknadId: Column<UUID> = uuid("soknad_id").references(SøknadTabell.søknadId)
    val seksjonsvar: Column<SeksjonsvarDAO> =
        json("seksjonsvar", { serializaSeksjonsvarDAO(it) }, { deserializaSeksjonsvarDAO(it) })
    val dokumentasjonskrav: Column<String?> = json("dokumentasjonskrav", { serializeSøknadData(it) }, { it }).nullable()
    val pdfGunnlag: Column<String> = json("pdf_grunnlag", { serializeSøknadData(it) }, { it })
    val opprettet: Column<LocalDateTime> = datetime("opprettet")
    val oppdatert: Column<LocalDateTime?> = datetime("oppdatert").nullable()
}

private fun serializeSøknadData(søknadData: String): String = objectMapper.writeValueAsString(søknadData)

private fun serializaSeksjonsvarDAO(seksjonsvarDAO: SeksjonsvarDAO): String = objectMapper.writeValueAsString(seksjonsvarDAO)

private fun deserializaSeksjonsvarDAO(seksjonsvarDAO: String): SeksjonsvarDAO =
    objectMapper.readValue<SeksjonsvarDAO>(
        seksjonsvarDAO,
    )

data class SeksjonsvarDAO(
    val seksjonId: String,
    val seksjonsvar: String,
    val versjon: Int,
) {
    companion object {
        fun fraJson(seksjonsvar: String): SeksjonsvarDAO = objectMapper.readValue<SeksjonsvarDAO>(seksjonsvar)
    }
}
