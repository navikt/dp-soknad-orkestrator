package no.nav.dagpenger.soknad.orkestrator.søknad.seksjon

import no.nav.dagpenger.soknad.orkestrator.søknad.Tilstand.PÅBEGYNT
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadTabell
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SortOrder.ASC
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.javatime.dateTimeLiteral
import org.jetbrains.exposed.sql.stringLiteral
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.upsert
import java.time.LocalDateTime.now
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
        seksjonsvar: String,
        dokumentasjonskrav: String? = null,
        pdfGrunnlag: String,
    ) {
        transaction {
            søknadRepository.verifiserAtSøknadEksistererOgTilhørerIdent(søknadId, ident)
            søknadRepository.verifiserAtSøknadHarForventetTilstand(søknadId, PÅBEGYNT)

            SeksjonV2Tabell.upsert(
                SeksjonV2Tabell.søknadId,
                SeksjonV2Tabell.seksjonId,
                onUpdate = {
                    it[SeksjonV2Tabell.seksjonsvar] = stringLiteral(seksjonsvar)
                    it[SeksjonV2Tabell.pdfGunnlag] = stringLiteral(pdfGrunnlag)
                    it[SeksjonV2Tabell.oppdatert] = dateTimeLiteral(now())
                    if (dokumentasjonskrav != null) {
                        it[SeksjonV2Tabell.dokumentasjonskrav] = stringLiteral(dokumentasjonskrav)
                    } else {
                        it[SeksjonV2Tabell.dokumentasjonskrav] = null
                    }
                },
            ) {
                it[SeksjonV2Tabell.søknadId] = søknadId
                it[SeksjonV2Tabell.seksjonId] = seksjonId
                it[SeksjonV2Tabell.seksjonsvar] = stringLiteral(seksjonsvar)
                if (dokumentasjonskrav != null) {
                    it[SeksjonV2Tabell.dokumentasjonskrav] = stringLiteral(dokumentasjonskrav)
                }
                it[SeksjonV2Tabell.pdfGunnlag] = stringLiteral(pdfGrunnlag)
            }

            søknadRepository.markerSøknadSomOppdatert(søknadId, ident)
        }
    }

    fun hentSeksjonsvar(
        ident: String,
        søknadId: UUID,
        seksjonId: String,
    ): String? =
        transaction {
            SeksjonV2Tabell
                .innerJoin(SøknadTabell)
                .select(SeksjonV2Tabell.seksjonsvar)
                .where {
                    SeksjonV2Tabell.søknadId eq søknadId and (SøknadTabell.ident eq ident) and (SeksjonV2Tabell.seksjonId eq seksjonId)
                }.map {
                    it[SeksjonV2Tabell.seksjonsvar]
                }.firstOrNull()
        }

    fun hentSeksjonsvarEllerKastException(
        ident: String,
        søknadId: UUID,
        seksjonId: String,
    ): String =
        transaction {
            hentSeksjonsvar(ident, søknadId, seksjonId)
                ?: throw IllegalStateException("Fant ingen seksjonsvar på $seksjonId for søknad $søknadId")
        }

    fun hentSeksjoner(
        ident: String,
        søknadId: UUID,
    ): List<Seksjon> =
        transaction {
            SeksjonV2Tabell
                .innerJoin(SøknadTabell)
                .select(SeksjonV2Tabell.seksjonsvar, SeksjonV2Tabell.seksjonId)
                .where { SeksjonV2Tabell.søknadId eq søknadId and (SøknadTabell.ident eq ident) }
                .map {
                    Seksjon(
                        seksjonId = it[SeksjonV2Tabell.seksjonId],
                        data = it[SeksjonV2Tabell.seksjonsvar],
                    )
                }.toList()
        }

    fun hentSeksjonIdForAlleLagredeSeksjoner(
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

    fun lagreDokumentasjonskrav(
        ident: String,
        søknadId: UUID,
        seksjonId: String,
        dokumentasjonskrav: String?,
    ) = transaction {
        søknadRepository.verifiserAtSøknadEksistererOgTilhørerIdent(søknadId, ident)
        søknadRepository.verifiserAtSøknadHarForventetTilstand(søknadId, PÅBEGYNT)
        requireNotNull(hentSeksjonsvar(ident, søknadId, seksjonId)) { "Fant ikke seksjon med ID $seksjonId" }

        SeksjonV2Tabell.update({ SeksjonV2Tabell.seksjonId eq seksjonId }) {
            if (dokumentasjonskrav != null) {
                it[SeksjonV2Tabell.dokumentasjonskrav] = stringLiteral(dokumentasjonskrav)
            } else {
                it[SeksjonV2Tabell.dokumentasjonskrav] = null
            }
            it[SeksjonV2Tabell.oppdatert] = dateTimeLiteral(now())
        }

        søknadRepository.markerSøknadSomOppdatert(søknadId, ident)
    }

    fun hentDokumentasjonskrav(
        ident: String,
        søknadId: UUID,
    ) = transaction {
        SeksjonV2Tabell
            .innerJoin(SøknadTabell)
            .select(SeksjonV2Tabell.dokumentasjonskrav)
            .where { SeksjonV2Tabell.søknadId eq søknadId and (SøknadTabell.ident eq ident) }
            .map {
                it[SeksjonV2Tabell.dokumentasjonskrav]
            }.toList()
            .filterNotNull()
    }

    fun hentDokumentasjonskrav(
        ident: String,
        søknadId: UUID,
        seksjonId: String,
    ) = transaction {
        SeksjonV2Tabell
            .innerJoin(SøknadTabell)
            .select(SeksjonV2Tabell.dokumentasjonskrav)
            .where { SeksjonV2Tabell.søknadId eq søknadId and (SøknadTabell.ident eq ident) and (SeksjonV2Tabell.seksjonId eq seksjonId) }
            .map {
                it[SeksjonV2Tabell.dokumentasjonskrav]
            }.firstOrNull()
    }

    fun hentPdfGrunnlag(
        ident: String,
        søknadId: UUID,
    ): List<String> =
        transaction {
            SeksjonV2Tabell
                .innerJoin(SøknadTabell)
                .select(SeksjonV2Tabell.pdfGunnlag)
                .where { SeksjonV2Tabell.søknadId eq søknadId and (SøknadTabell.ident eq ident) }
                .orderBy(SeksjonV2Tabell.opprettet, ASC)
                .map {
                    it[SeksjonV2Tabell.pdfGunnlag]
                }.toList()
        }

    fun slettAlleSeksjoner(
        ident: String,
        søknadId: UUID,
    ) = transaction {
        søknadRepository.verifiserAtSøknadEksistererOgTilhørerIdent(søknadId, ident)
        SeksjonV2Tabell.deleteWhere { SeksjonV2Tabell.søknadId eq søknadId }
    }
}
