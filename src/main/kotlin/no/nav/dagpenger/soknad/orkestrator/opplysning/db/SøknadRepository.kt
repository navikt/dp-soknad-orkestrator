package no.nav.dagpenger.soknad.orkestrator.opplysning.db

import SøknadTabell
import java.util.UUID
import javax.sql.DataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class SøknadRepository(dataSource: DataSource) {
    val database = Database.connect(dataSource)

    fun lagre(søknad: Søknad) {
        transaction {
            SøknadTabell.insert {
                it[søknadId] = søknad.søknadId
                it[ident] = søknad.ident
            }
        }
    }

    fun hentSøknad(søknadId: UUID): Søknad? {
        return transaction {
            SøknadTabell
                .selectAll()
                .where { SøknadTabell.søknadId eq søknadId }
                .map {
                    Søknad(
                        søknadId = it[SøknadTabell.søknadId],
                        ident = it[SøknadTabell.ident]
                    )
                }
                .firstOrNull()
        }
    }
}
