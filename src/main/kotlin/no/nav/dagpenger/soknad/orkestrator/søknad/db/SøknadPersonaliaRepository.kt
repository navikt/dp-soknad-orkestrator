package no.nav.dagpenger.soknad.orkestrator.søknad.db

import no.nav.dagpenger.soknad.orkestrator.søknad.SøknadPersonalia
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.upsert
import java.util.UUID
import javax.sql.DataSource

class SøknadPersonaliaRepository(
    dataSource: DataSource,
) {
    val database = Database.connect(dataSource)

    fun lagre(søknadPersonalia: SøknadPersonalia) {
        transaction {
            SøknadPersonaliaTabell.upsert(
                SøknadPersonaliaTabell.søknadId,
                SøknadPersonaliaTabell.ident,
            ) {
                it[SøknadPersonaliaTabell.søknadId] = søknadPersonalia.søknadId
                it[SøknadPersonaliaTabell.ident] = søknadPersonalia.ident
                it[SøknadPersonaliaTabell.fornavn] = søknadPersonalia.fornavn
                it[SøknadPersonaliaTabell.mellomnavn] = søknadPersonalia.mellomnavn
                it[SøknadPersonaliaTabell.etternavn] = søknadPersonalia.etternavn
                it[SøknadPersonaliaTabell.alder] = søknadPersonalia.alder
                it[SøknadPersonaliaTabell.adresselinje1] = søknadPersonalia.adresselinje1
                it[SøknadPersonaliaTabell.adresselinje2] = søknadPersonalia.adresselinje2
                it[SøknadPersonaliaTabell.adresselinje3] = søknadPersonalia.adresselinje3
                it[SøknadPersonaliaTabell.postnummer] = søknadPersonalia.postnummer
                it[SøknadPersonaliaTabell.poststed] = søknadPersonalia.poststed
                it[SøknadPersonaliaTabell.landkode] = søknadPersonalia.landkode
                it[SøknadPersonaliaTabell.land] = søknadPersonalia.land
                it[SøknadPersonaliaTabell.kontonummer] = søknadPersonalia.kontonummer
            }
        }
    }

    fun hent(
        søknadId: UUID,
        ident: String,
    ) = transaction {
        SøknadPersonaliaTabell
            .selectAll()
            .where { SøknadPersonaliaTabell.søknadId eq søknadId and (SøknadPersonaliaTabell.ident eq ident) }
            .map {
                SøknadPersonalia(
                    søknadId = it[SøknadPersonaliaTabell.søknadId],
                    ident = it[SøknadPersonaliaTabell.ident],
                    fornavn = it[SøknadPersonaliaTabell.fornavn],
                    mellomnavn = it[SøknadPersonaliaTabell.mellomnavn],
                    etternavn = it[SøknadPersonaliaTabell.etternavn],
                    alder = it[SøknadPersonaliaTabell.alder],
                    adresselinje1 = it[SøknadPersonaliaTabell.adresselinje1],
                    adresselinje2 = it[SøknadPersonaliaTabell.adresselinje2],
                    adresselinje3 = it[SøknadPersonaliaTabell.adresselinje3],
                    postnummer = it[SøknadPersonaliaTabell.postnummer],
                    poststed = it[SøknadPersonaliaTabell.poststed],
                    landkode = it[SøknadPersonaliaTabell.landkode],
                    land = it[SøknadPersonaliaTabell.land],
                    kontonummer = it[SøknadPersonaliaTabell.kontonummer],
                )
            }.firstOrNull()
    }
}

object SøknadPersonaliaTabell : IntIdTable("soknad_personalia") {
    val søknadId: Column<UUID> = uuid("soknad_id")
    val ident: Column<String> = varchar("ident", 11)
    val fornavn: Column<String> = varchar("fornavn", 255)
    val mellomnavn: Column<String?> = varchar("mellomnavn", 255).nullable()
    val etternavn: Column<String> = varchar("etternavn", 255)
    val alder: Column<String> = varchar("alder", 3)
    val adresselinje1: Column<String?> = varchar("adresselinje1", 255).nullable()
    val adresselinje2: Column<String?> = varchar("adresselinje2", 255).nullable()
    val adresselinje3: Column<String?> = varchar("adresselinje3", 255).nullable()
    val postnummer: Column<String?> = varchar("postnummer", 255).nullable()
    val poststed: Column<String?> = varchar("poststed", 255).nullable()
    val landkode: Column<String?> = varchar("landkode", 3).nullable()
    val land: Column<String?> = varchar("land", 255).nullable()
    val kontonummer: Column<String?> = varchar("kontonummer", 255).nullable()
}
