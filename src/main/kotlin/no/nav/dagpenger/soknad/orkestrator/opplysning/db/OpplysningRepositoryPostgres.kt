package no.nav.dagpenger.soknad.orkestrator.opplysning.db

import OpplysningTabell
import no.nav.dagpenger.soknad.orkestrator.opplysning.Arbeidsforhold
import no.nav.dagpenger.soknad.orkestrator.opplysning.ArbeidsforholdSvar
import no.nav.dagpenger.soknad.orkestrator.opplysning.Boolsk
import no.nav.dagpenger.soknad.orkestrator.opplysning.Dato
import no.nav.dagpenger.soknad.orkestrator.opplysning.Desimaltall
import no.nav.dagpenger.soknad.orkestrator.opplysning.EøsArbeidsforhold
import no.nav.dagpenger.soknad.orkestrator.opplysning.EøsArbeidsforholdSvar
import no.nav.dagpenger.soknad.orkestrator.opplysning.Flervalg
import no.nav.dagpenger.soknad.orkestrator.opplysning.Heltall
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysning
import no.nav.dagpenger.soknad.orkestrator.opplysning.Periode
import no.nav.dagpenger.soknad.orkestrator.opplysning.PeriodeSvar
import no.nav.dagpenger.soknad.orkestrator.opplysning.Tekst
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource

class OpplysningRepositoryPostgres(dataSource: DataSource) : OpplysningRepository {
    val database = Database.connect(dataSource)

    override fun lagre(opplysning: Opplysning<*>) {
        transaction {
            if (!opplysningEksisterer(opplysning)) {
                OpplysningTabell.leggTil(opplysning)

                val opplysningId =
                    OpplysningTabell.selectAll().somMatcher(
                        opplysning.beskrivendeId,
                        opplysning.ident,
                        opplysning.søknadsId,
                    ).first()[OpplysningTabell.id].value

                when (opplysning.type) {
                    Tekst ->
                        TekstTabell.insert {
                            it[TekstTabell.opplysningId] = opplysningId
                            it[svar] = opplysning.svar as String
                        }

                    Heltall ->
                        HeltallTabell.insert {
                            it[HeltallTabell.opplysningId] = opplysningId
                            it[svar] = opplysning.svar as Int
                        }

                    Desimaltall ->
                        DesimaltallTabell.insert {
                            it[DesimaltallTabell.opplysningId] = opplysningId
                            it[svar] = opplysning.svar as Double
                        }

                    Boolsk ->
                        BoolskTabell.insert {
                            it[BoolskTabell.opplysningId] = opplysningId
                            it[svar] = opplysning.svar as Boolean
                        }

                    Dato ->
                        DatoTabell.insert {
                            it[DatoTabell.opplysningId] = opplysningId
                            it[svar] = opplysning.svar as LocalDate
                        }

                    Flervalg -> {
                        val flervalgId =
                            FlervalgTabell.insertAndGetId {
                                it[FlervalgTabell.opplysningId] = opplysningId
                            }.value

                        (opplysning.svar as List<String>).forEach { flervalgSvar ->
                            FlervalgSvarTabell.insert {
                                it[FlervalgSvarTabell.flervalgId] = flervalgId
                                it[svar] = flervalgSvar
                            }
                        }
                    }

                    Periode ->
                        (opplysning.svar as PeriodeSvar).also { periodeSvar ->
                            PeriodeTabell.insert {
                                it[PeriodeTabell.opplysningId] = opplysningId
                                it[fom] = periodeSvar.fom
                                it[tom] = periodeSvar.tom
                            }
                        }

                    Arbeidsforhold -> {
                        val arbeidsforholdId =
                            ArbeidsforholdTabell.insertAndGetId {
                                it[ArbeidsforholdTabell.opplysningId] = opplysningId
                            }.value

                        (opplysning.svar as List<ArbeidsforholdSvar>).forEach { arbeidsforholdSvar ->
                            val navnSvarId =
                                TekstTabell.insertAndGetId {
                                    it[TekstTabell.opplysningId] = opplysningId
                                    it[svar] = arbeidsforholdSvar.navn
                                }.value

                            val landSvarId =
                                TekstTabell.insertAndGetId {
                                    it[TekstTabell.opplysningId] = opplysningId
                                    it[svar] = arbeidsforholdSvar.land
                                }.value

                            ArbeidsforholdSvarTabell.insert {
                                it[ArbeidsforholdSvarTabell.arbeidsforholdId] = arbeidsforholdId
                                it[this.navnSvarId] = navnSvarId
                                it[this.landSvarId] = landSvarId
                            }
                        }
                    }

                    EøsArbeidsforhold -> {
                        val arbeidsforholdId =
                            ArbeidsforholdTabell.insertAndGetId {
                                it[ArbeidsforholdTabell.opplysningId] = opplysningId
                            }.value

                        (opplysning.svar as List<EøsArbeidsforholdSvar>).forEach { eøsArbeidsforholdSvar ->
                            val bedriftnavnSvarId =
                                TekstTabell.insertAndGetId {
                                    it[TekstTabell.opplysningId] = opplysningId
                                    it[svar] = eøsArbeidsforholdSvar.bedriftnavn
                                }.value

                            val landSvarId =
                                TekstTabell.insertAndGetId {
                                    it[TekstTabell.opplysningId] = opplysningId
                                    it[svar] = eøsArbeidsforholdSvar.land
                                }.value

                            val personnummerSvarId =
                                TekstTabell.insertAndGetId {
                                    it[TekstTabell.opplysningId] = opplysningId
                                    it[svar] = eøsArbeidsforholdSvar.personnummerIArbeidsland
                                }.value

                            val varighetSvarId =
                                PeriodeTabell.insertAndGetId {
                                    it[PeriodeTabell.opplysningId] = opplysningId
                                    it[fom] = eøsArbeidsforholdSvar.varighet.fom
                                    it[tom] = eøsArbeidsforholdSvar.varighet.tom
                                }.value

                            EøsArbeidsforholdSvarTabell.insert {
                                it[EøsArbeidsforholdSvarTabell.arbeidsforholdId] = arbeidsforholdId
                                it[this.bedriftnavnSvarId] = bedriftnavnSvarId
                                it[this.landSvarId] = landSvarId
                                it[this.personnummerSvarId] = personnummerSvarId
                                it[this.varighetSvarId] = varighetSvarId
                            }
                        }
                    }
                }
            }
        }
    }

    override fun hent(
        beskrivendeId: String,
        ident: String,
        søknadsId: UUID,
    ): Opplysning<*> {
        return transaction {
            OpplysningTabell
                .selectAll()
                .somMatcher(beskrivendeId, ident, søknadsId)
                .map(tilOpplysning())
                .firstOrNull()
                ?: throw NoSuchElementException(
                    "Ingen opplysning funnet med gitt beskrivendeId:" + " $beskrivendeId," +
                        " ident: $ident, " +
                        "og søknadsId: $søknadsId",
                )
        }
    }
}

private fun opplysningEksisterer(opplysning: Opplysning<*>): Boolean =
    OpplysningTabell.selectAll().somMatcher(
        opplysning.beskrivendeId,
        opplysning.ident,
        opplysning.søknadsId,
    ).any()

fun OpplysningTabell.leggTil(opplysning: Opplysning<*>) {
    insert {
        it[beskrivendeId] = opplysning.beskrivendeId
        it[type] = opplysning.type::class.java.simpleName
        it[ident] = opplysning.ident
        it[søknadsId] = opplysning.søknadsId
    }
}

fun Query.somMatcher(
    beskrivendeId: String,
    ident: String,
    søknadsId: UUID,
): Query =
    where {
        OpplysningTabell.beskrivendeId eq beskrivendeId and
            (OpplysningTabell.ident eq ident) and
            (OpplysningTabell.søknadsId eq søknadsId)
    }

private fun tilOpplysning(): (ResultRow) -> Opplysning<*> =
    {
        when (it[OpplysningTabell.type]) {
            "Tekst" -> tilTekstOpplysning(it)
            "Heltall" -> tilHeltallOpplysning(it)
            "Desimaltall" -> tilDesimaltallOpplysning(it)
            "Boolsk" -> tilBoolskOpplysning(it)
            "Dato" -> tilDatoOpplysning(it)
            "Flervalg" -> tilFlervalgOpplysning(it)
            "Periode" -> tilPeriodeOpplysning(it)
            "Arbeidsforhold" -> tilArbeidsforholdOpplysning(it)
            "EøsArbeidsforhold" -> tilEøsArbeidsforholdOpplysning(it)
            else -> throw IllegalArgumentException("Ukjent datatype: ${it[OpplysningTabell.type]}")
        }
    }

private fun tilTekstOpplysning(it: ResultRow) =
    Opplysning(
        beskrivendeId = it[OpplysningTabell.beskrivendeId],
        type = Tekst,
        svar = hentTekstSvar(it),
        ident = it[OpplysningTabell.ident],
        søknadsId = it[OpplysningTabell.søknadsId],
    )

private fun hentTekstSvar(it: ResultRow): String =
    TekstTabell
        .select(TekstTabell.svar)
        .where { TekstTabell.opplysningId eq it[OpplysningTabell.id].value }.first()[TekstTabell.svar]

private fun tilHeltallOpplysning(it: ResultRow) =
    Opplysning(
        beskrivendeId = it[OpplysningTabell.beskrivendeId],
        type = Heltall,
        svar = hentHeltallSvar(it),
        ident = it[OpplysningTabell.ident],
        søknadsId = it[OpplysningTabell.søknadsId],
    )

private fun hentHeltallSvar(it: ResultRow): Int =
    HeltallTabell
        .select(HeltallTabell.svar)
        .where { HeltallTabell.opplysningId eq it[OpplysningTabell.id].value }.first()[HeltallTabell.svar]

private fun tilDesimaltallOpplysning(it: ResultRow) =
    Opplysning(
        beskrivendeId = it[OpplysningTabell.beskrivendeId],
        type = Desimaltall,
        svar = hentDesimaltallSvar(it),
        ident = it[OpplysningTabell.ident],
        søknadsId = it[OpplysningTabell.søknadsId],
    )

private fun hentDesimaltallSvar(it: ResultRow): Double =
    DesimaltallTabell
        .select(DesimaltallTabell.svar)
        .where { DesimaltallTabell.opplysningId eq it[OpplysningTabell.id].value }.first()[DesimaltallTabell.svar]

private fun tilBoolskOpplysning(it: ResultRow) =
    Opplysning(
        beskrivendeId = it[OpplysningTabell.beskrivendeId],
        type = Boolsk,
        svar = hentBoolskSvar(it),
        ident = it[OpplysningTabell.ident],
        søknadsId = it[OpplysningTabell.søknadsId],
    )

private fun hentBoolskSvar(it: ResultRow): Boolean =
    BoolskTabell
        .select(BoolskTabell.svar)
        .where { BoolskTabell.opplysningId eq it[OpplysningTabell.id].value }.first()[BoolskTabell.svar]

private fun tilDatoOpplysning(it: ResultRow) =
    Opplysning(
        beskrivendeId = it[OpplysningTabell.beskrivendeId],
        type = Dato,
        svar = hentDatoSvar(it),
        ident = it[OpplysningTabell.ident],
        søknadsId = it[OpplysningTabell.søknadsId],
    )

private fun hentDatoSvar(it: ResultRow): LocalDate =
    DatoTabell
        .select(DatoTabell.svar)
        .where { DatoTabell.opplysningId eq it[OpplysningTabell.id].value }.first()[DatoTabell.svar]

private fun tilFlervalgOpplysning(it: ResultRow) =
    Opplysning(
        beskrivendeId = it[OpplysningTabell.beskrivendeId],
        type = Flervalg,
        svar = hentFlervalgSvar(it),
        ident = it[OpplysningTabell.ident],
        søknadsId = it[OpplysningTabell.søknadsId],
    )

private fun hentFlervalgSvar(it: ResultRow): List<String> {
    val flervalgId =
        FlervalgTabell
            .select(FlervalgTabell.id)
            .where { FlervalgTabell.opplysningId eq it[OpplysningTabell.id].value }.first()[FlervalgTabell.id].value

    return FlervalgSvarTabell
        .select(FlervalgSvarTabell.svar)
        .where { FlervalgSvarTabell.flervalgId eq flervalgId }
        .map { it[FlervalgSvarTabell.svar] }
}

private fun tilPeriodeOpplysning(it: ResultRow) =
    Opplysning(
        beskrivendeId = it[OpplysningTabell.beskrivendeId],
        type = Periode,
        svar = hentPeriodeSvar(it),
        ident = it[OpplysningTabell.ident],
        søknadsId = it[OpplysningTabell.søknadsId],
    )

private fun hentPeriodeSvar(it: ResultRow): PeriodeSvar {
    return PeriodeTabell
        .select(PeriodeTabell.fom, PeriodeTabell.tom)
        .where(PeriodeTabell.opplysningId eq it[OpplysningTabell.id].value)
        .map {
            PeriodeSvar(
                fom = it[PeriodeTabell.fom],
                tom = it[PeriodeTabell.tom],
            )
        }.first()
}

private fun tilArbeidsforholdOpplysning(it: ResultRow) =
    Opplysning(
        beskrivendeId = it[OpplysningTabell.beskrivendeId],
        type = Arbeidsforhold,
        svar = hentArbeidsforholdSvar(it),
        ident = it[OpplysningTabell.ident],
        søknadsId = it[OpplysningTabell.søknadsId],
    )

fun hentArbeidsforholdSvar(it: ResultRow): List<ArbeidsforholdSvar> {
    val arbeidsforholdId =
        ArbeidsforholdTabell
            .select(ArbeidsforholdTabell.id)
            .where { ArbeidsforholdTabell.opplysningId eq it[OpplysningTabell.id].value }
            .first()[ArbeidsforholdTabell.id].value

    return ArbeidsforholdSvarTabell
        .select(ArbeidsforholdSvarTabell.navnSvarId, ArbeidsforholdSvarTabell.landSvarId)
        .where { ArbeidsforholdSvarTabell.arbeidsforholdId eq arbeidsforholdId }
        .map {
            ArbeidsforholdSvar(
                navn =
                    TekstTabell
                        .select(TekstTabell.svar)
                        .where { TekstTabell.id eq it[ArbeidsforholdSvarTabell.navnSvarId] }
                        .first()[TekstTabell.svar],
                land =
                    TekstTabell
                        .select(TekstTabell.svar)
                        .where { TekstTabell.id eq it[ArbeidsforholdSvarTabell.landSvarId] }
                        .first()[TekstTabell.svar],
            )
        }
}

private fun tilEøsArbeidsforholdOpplysning(it: ResultRow) =
    Opplysning(
        beskrivendeId = it[OpplysningTabell.beskrivendeId],
        type = EøsArbeidsforhold,
        svar = hentEøsArbeidsforholdSvar(it),
        ident = it[OpplysningTabell.ident],
        søknadsId = it[OpplysningTabell.søknadsId],
    )

fun hentEøsArbeidsforholdSvar(it: ResultRow): List<EøsArbeidsforholdSvar> {
    val arbeidsforholdId =
        ArbeidsforholdTabell
            .select(ArbeidsforholdTabell.id)
            .where { ArbeidsforholdTabell.opplysningId eq it[OpplysningTabell.id].value }
            .first()[ArbeidsforholdTabell.id].value

    return EøsArbeidsforholdSvarTabell
        .select(
            EøsArbeidsforholdSvarTabell.bedriftnavnSvarId,
            EøsArbeidsforholdSvarTabell.landSvarId,
            EøsArbeidsforholdSvarTabell.personnummerSvarId,
            EøsArbeidsforholdSvarTabell.varighetSvarId,
        )
        .where { EøsArbeidsforholdSvarTabell.arbeidsforholdId eq arbeidsforholdId }
        .map {
            EøsArbeidsforholdSvar(
                bedriftnavn =
                    TekstTabell
                        .select(TekstTabell.svar)
                        .where { TekstTabell.id eq it[EøsArbeidsforholdSvarTabell.bedriftnavnSvarId] }
                        .first()[TekstTabell.svar],
                land =
                    TekstTabell
                        .select(TekstTabell.svar)
                        .where { TekstTabell.id eq it[EøsArbeidsforholdSvarTabell.landSvarId] }
                        .first()[TekstTabell.svar],
                personnummerIArbeidsland =
                    TekstTabell
                        .select(TekstTabell.svar)
                        .where { TekstTabell.id eq it[EøsArbeidsforholdSvarTabell.personnummerSvarId] }
                        .first()[TekstTabell.svar],
                varighet =
                    PeriodeTabell
                        .select(PeriodeTabell.fom, PeriodeTabell.tom)
                        .where { PeriodeTabell.id eq it[EøsArbeidsforholdSvarTabell.varighetSvarId] }
                        .map {
                            PeriodeSvar(
                                fom = it[PeriodeTabell.fom],
                                tom = it[PeriodeTabell.tom],
                            )
                        }.first(),
            )
        }
}
