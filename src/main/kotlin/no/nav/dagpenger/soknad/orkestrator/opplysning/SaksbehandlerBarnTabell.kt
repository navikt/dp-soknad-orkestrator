package no.nav.dagpenger.soknad.orkestrator.opplysning

import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadTabell
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.timestampWithTimeZone
import org.jetbrains.exposed.sql.json.json
import java.time.OffsetDateTime
import java.util.UUID

object SaksbehandlerBarnTabell : LongIdTable("saksbehandler_barn") {
    val søknadId: Column<UUID> = uuid("soknad_id").references(SøknadTabell.søknadId)
    val barn: Column<String> = json<String>("barn", { it }, { it })
    val endretAv: Column<String> = text("endret_av")
    val opprettet: Column<OffsetDateTime> = timestampWithTimeZone("opprettet")
}
