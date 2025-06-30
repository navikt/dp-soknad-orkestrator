package no.nav.dagpenger.soknad.orkestrator.søknad.seksjon

import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadTabell
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.json.jsonb
import java.util.UUID

object SeksjonTabell : UUIDTable("seksjon") {
    val seksjonId: Column<String> = text("seksjon_id")
    val søknadId: Column<UUID> = uuid("soknad_id").references(SøknadTabell.søknadId)
    val json: Column<String> = jsonb("json", { serializeSøknadData(it) }, { it })
}

private fun serializeSøknadData(søknadData: String): String = objectMapper.writeValueAsString(søknadData)
