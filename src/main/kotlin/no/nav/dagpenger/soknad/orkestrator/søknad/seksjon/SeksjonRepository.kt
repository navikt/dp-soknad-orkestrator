package no.nav.dagpenger.soknad.orkestrator.søknad.seksjon

import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadTabell
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.stringLiteral
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.upsert
import java.util.UUID
import javax.sql.DataSource

class SeksjonRepository(
    dataSource: DataSource,
    val søknadRepository: SøknadRepository,
) {
    val database = Database.connect(dataSource)

    fun lagre(
        ident: String,
        søknadId: UUID,
        seksjonId: String,
        json: String,
    ) {
        transaction {
            val søknad = søknadRepository.hent(søknadId)
            requireNotNull(søknad) { "Fant ikke søknad med ID $søknadId." }
            require(søknad.ident == ident) { "Søknad $søknadId tilhører ikke identen som prøver å lagre seksjonen" }

            SeksjonV2Tabell.upsert(
                SeksjonV2Tabell.søknadId,
                SeksjonV2Tabell.seksjonId,
                onUpdate = listOf(Pair(SeksjonV2Tabell.json, stringLiteral(json))),
            ) {
                it[SeksjonV2Tabell.søknadId] = søknadId
                it[SeksjonV2Tabell.seksjonId] = seksjonId
                it[SeksjonV2Tabell.json] = stringLiteral(json)
            }
        }
    }

    fun hent(
        ident: String,
        søknadId: UUID,
        seksjonId: String,
    ): String? =
        transaction {
            SeksjonV2Tabell
                .innerJoin(SøknadTabell)
                .select(SeksjonV2Tabell.json)
                .where {
                    SeksjonV2Tabell.søknadId eq søknadId and (SøknadTabell.ident eq ident) and (SeksjonV2Tabell.seksjonId eq seksjonId)
                }.map {
                    it[SeksjonV2Tabell.json]
                }.firstOrNull()
        }

    fun hentSeksjoner(
        ident: String,
        søknadId: UUID,
    ): List<Seksjon> =
        transaction {
            SeksjonV2Tabell
                .innerJoin(SøknadTabell)
                .select(SeksjonV2Tabell.json, SeksjonV2Tabell.seksjonId)
                .where { SeksjonV2Tabell.søknadId eq søknadId and (SøknadTabell.ident eq ident) }
                .map {
                    Seksjon(
                        seksjonId = it[SeksjonV2Tabell.seksjonId],
                        data = it[SeksjonV2Tabell.json],
                    )
                }.toList()
        }

    fun hentFullførteSeksjoner(
        ident: String,
        søknadId: UUID,
    ): List<String> =
        transaction {
            SeksjonV2Tabell
                .innerJoin(SøknadTabell)
                .select(SeksjonV2Tabell.seksjonId)
                .where { SeksjonV2Tabell.søknadId eq søknadId and (SøknadTabell.ident eq ident) }
                .map {
                    it[SeksjonV2Tabell.seksjonId]
                }.toList()
        }
}
