package no.nav.dagpenger.soknad.orkestrator.søknad.seksjon

import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadTabell
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import java.util.UUID

@Suppress("ExposedReference")
object SeksjonTabell : IntIdTable("seksjon") {
    val seksjonId: Column<String> = text("seksjon_id")
    val søknadId: Column<UUID> = uuid("soknad_id").references(SøknadTabell.søknadId) // TODO hvilken ID skal vi referere her?
    val json: Column<String> = text("json")
}
