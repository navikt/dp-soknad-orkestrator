package no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db

import ArbeidsforholdSvarTabell
import ArbeidsforholdTabell
import BarnSvarTabell
import BarnSøknadMappingTabell
import BarnTabell
import BoolskTabell
import DatoTabell
import DesimaltallTabell
import EgenNæringSvarTabell
import EgenNæringTabell
import EøsArbeidsforholdSvarTabell
import FlervalgSvarTabell
import FlervalgTabell
import HeltallTabell
import PeriodeTabell
import QuizOpplysningTabell
import TekstTabell
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.QuizOpplysning
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.asListOf
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Arbeidsforhold
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.ArbeidsforholdSvar
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Barn
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.BarnSvar
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Boolsk
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Dato
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Desimaltall
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.EgenNæring
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.EøsArbeidsforhold
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.EøsArbeidsforholdSvar
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Flervalg
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Heltall
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Periode
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.PeriodeSvar
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Sluttårsak
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Tekst
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import java.util.UUID.randomUUID
import javax.sql.DataSource

class QuizOpplysningRepositoryPostgres(
    dataSource: DataSource,
) : QuizOpplysningRepository {
    val database = Database.connect(dataSource)

    override fun lagre(opplysning: QuizOpplysning<*>) {
        transaction {
            if (!opplysningEksisterer(opplysning)) {
                val opplysningId = QuizOpplysningTabell.insertAndGetId(opplysning)

                when (opplysning.type) {
                    Tekst -> {
                        lagreTekstSvar(opplysningId, opplysning.svar as String)
                    }

                    Heltall -> {
                        lagreHeltallSvar(opplysningId, opplysning.svar as Int)
                    }

                    Desimaltall -> {
                        lagreDesimaltallSvar(opplysningId, opplysning.svar as Double)
                    }

                    Boolsk -> {
                        lagreBoolskSvar(opplysningId, opplysning.svar as Boolean)
                    }

                    Dato -> {
                        lagreDatoSvar(opplysningId, opplysning.svar as LocalDate)
                    }

                    Flervalg -> {
                        lagreFlervalgSvar(opplysningId, opplysning.svar.asListOf<String>())
                    }

                    Periode -> {
                        lagrePeriodeSvar(opplysningId, opplysning.svar as PeriodeSvar)
                    }

                    Arbeidsforhold -> {
                        lagreArbeidsforholdSvar(
                            opplysningId,
                            opplysning.svar.asListOf<ArbeidsforholdSvar>(),
                        )
                    }

                    EøsArbeidsforhold -> {
                        lagreEøsArbeidsforholdSvar(
                            opplysningId,
                            opplysning.svar.asListOf<EøsArbeidsforholdSvar>(),
                        )
                    }

                    EgenNæring -> {
                        lagreEgenNæringSvar(opplysningId, opplysning.svar.asListOf<Int>())
                    }

                    Barn -> {
                        lagreBarnSvar(opplysningId, opplysning.svar.asListOf<BarnSvar>())
                    }
                }
            }
        }
    }

    override fun hent(
        beskrivendeId: String,
        ident: String,
        søknadId: UUID,
    ): QuizOpplysning<*>? =
        transaction {
            QuizOpplysningTabell
                .selectAll()
                .somMatcher(beskrivendeId, ident, søknadId)
                .map(tilOpplysning())
                .firstOrNull()
        }

    override fun hent(
        beskrivendeId: String,
        søknadId: UUID,
    ): QuizOpplysning<*>? = hentAlle(søknadId).find { it.beskrivendeId == beskrivendeId }

    override fun hentAlle(søknadId: UUID): List<QuizOpplysning<*>> =
        transaction {
            QuizOpplysningTabell
                .selectAll()
                .where { QuizOpplysningTabell.søknadId eq søknadId }
                .map(tilOpplysning())
        }

    override fun slett(søknadId: UUID) {
        transaction {
            QuizOpplysningTabell.deleteWhere { QuizOpplysningTabell.søknadId eq søknadId }
        }
    }

    override fun oppdaterBarn(
        søknadId: UUID,
        oppdatertBarn: BarnSvar,
    ) {
        transaction {
            BarnSvarTabell.update({ BarnSvarTabell.barnSvarId eq oppdatertBarn.barnSvarId }) {
                it[fornavnMellomnavn] = oppdatertBarn.fornavnOgMellomnavn
                it[etternavn] = oppdatertBarn.etternavn
                it[fødselsdato] = oppdatertBarn.fødselsdato
                it[statsborgerskap] = oppdatertBarn.statsborgerskap
                it[forsørgerBarnet] = oppdatertBarn.forsørgerBarnet
                it[fraRegister] = oppdatertBarn.fraRegister
                it[kvalifisererTilBarnetillegg] = oppdatertBarn.kvalifisererTilBarnetillegg
                it[barnetilleggFom] = oppdatertBarn.barnetilleggFom
                it[barnetilleggTom] = oppdatertBarn.barnetilleggTom
                it[endretAv] = oppdatertBarn.endretAv
                it[begrunnelse] = oppdatertBarn.begrunnelse
                it[sistEndret] = OffsetDateTime.now(ZoneOffset.UTC)
            }
        }
    }

    override fun lagreBarnSøknadMapping(søknadId: UUID) = hentEllerOpprettSøknadbarnId(søknadId)

    override fun hentEllerOpprettSøknadbarnId(søknadId: UUID): UUID =
        transaction {
            var søknadbarnId =
                BarnSøknadMappingTabell
                    .select(BarnSøknadMappingTabell.id, BarnSøknadMappingTabell.søknadbarnId)
                    .where { BarnSøknadMappingTabell.søknadId eq søknadId }
                    .firstOrNull()
                    ?.get(BarnSøknadMappingTabell.søknadbarnId)

            if (søknadbarnId == null) {
                søknadbarnId = randomUUID()

                BarnSøknadMappingTabell.insert {
                    it[BarnSøknadMappingTabell.søknadId] = søknadId
                    it[BarnSøknadMappingTabell.søknadbarnId] = søknadbarnId
                }
            }

            søknadbarnId
        }

    override fun mapTilSøknadId(søknadbarnId: UUID): UUID? =
        transaction {
            BarnSøknadMappingTabell
                .select(BarnSøknadMappingTabell.søknadId)
                .where { BarnSøknadMappingTabell.søknadbarnId eq søknadbarnId }
                .firstOrNull()
                ?.get(BarnSøknadMappingTabell.søknadId)
        }
}

private fun opplysningEksisterer(opplysning: QuizOpplysning<*>): Boolean =
    QuizOpplysningTabell
        .selectAll()
        .somMatcher(
            opplysning.beskrivendeId,
            opplysning.ident,
            opplysning.søknadId,
        ).any()

fun QuizOpplysningTabell.insertAndGetId(opplysning: QuizOpplysning<*>) =
    insertAndGetId {
        it[beskrivendeId] = opplysning.beskrivendeId
        it[type] = opplysning.type::class.java.simpleName
        it[ident] = opplysning.ident
        it[søknadId] = opplysning.søknadId
    }.value

fun Query.somMatcher(
    beskrivendeId: String,
    ident: String,
    søknadId: UUID,
): Query =
    where {
        QuizOpplysningTabell.beskrivendeId eq beskrivendeId and
            (QuizOpplysningTabell.ident eq ident) and
            (QuizOpplysningTabell.søknadId eq søknadId)
    }

private fun tilOpplysning(): (ResultRow) -> QuizOpplysning<*> =
    {
        when (it[QuizOpplysningTabell.type]) {
            "Tekst" -> tilTekstOpplysning(it)
            "Heltall" -> tilHeltallOpplysning(it)
            "Desimaltall" -> tilDesimaltallOpplysning(it)
            "Boolsk" -> tilBoolskOpplysning(it)
            "Dato" -> tilDatoOpplysning(it)
            "Flervalg" -> tilFlervalgOpplysning(it)
            "Periode" -> tilPeriodeOpplysning(it)
            "Arbeidsforhold" -> tilArbeidsforholdOpplysning(it)
            "EøsArbeidsforhold" -> tilEøsArbeidsforholdOpplysning(it)
            "EgenNæring" -> tilEgenNæringOpplysning(it)
            "Barn" -> tilBarnOpplysning(it)
            else -> throw IllegalArgumentException("Ukjent datatype: ${it[QuizOpplysningTabell.type]}")
        }
    }

private fun tilTekstOpplysning(it: ResultRow) =
    QuizOpplysning(
        beskrivendeId = it[QuizOpplysningTabell.beskrivendeId],
        type = Tekst,
        svar = hentTekstSvar(it),
        ident = it[QuizOpplysningTabell.ident],
        søknadId = it[QuizOpplysningTabell.søknadId],
    )

private fun lagreTekstSvar(
    opplysningId: Int,
    svar: String,
) {
    TekstTabell.insert {
        it[TekstTabell.quizOpplysningId] = opplysningId
        it[TekstTabell.svar] = svar
    }
}

private fun hentTekstSvar(it: ResultRow): String =
    TekstTabell
        .select(TekstTabell.svar)
        .where { TekstTabell.quizOpplysningId eq it[QuizOpplysningTabell.id].value }
        .first()[TekstTabell.svar]

private fun tilHeltallOpplysning(it: ResultRow) =
    QuizOpplysning(
        beskrivendeId = it[QuizOpplysningTabell.beskrivendeId],
        type = Heltall,
        svar = hentHeltallSvar(it),
        ident = it[QuizOpplysningTabell.ident],
        søknadId = it[QuizOpplysningTabell.søknadId],
    )

private fun lagreHeltallSvar(
    opplysningId: Int,
    svar: Int,
) {
    HeltallTabell.insert {
        it[HeltallTabell.quizOpplysningId] = opplysningId
        it[HeltallTabell.svar] = svar
    }
}

private fun hentHeltallSvar(it: ResultRow): Int =
    HeltallTabell
        .select(HeltallTabell.svar)
        .where { HeltallTabell.quizOpplysningId eq it[QuizOpplysningTabell.id].value }
        .first()[HeltallTabell.svar]

private fun tilDesimaltallOpplysning(it: ResultRow) =
    QuizOpplysning(
        beskrivendeId = it[QuizOpplysningTabell.beskrivendeId],
        type = Desimaltall,
        svar = hentDesimaltallSvar(it),
        ident = it[QuizOpplysningTabell.ident],
        søknadId = it[QuizOpplysningTabell.søknadId],
    )

private fun lagreDesimaltallSvar(
    opplysningId: Int,
    svar: Double,
) {
    DesimaltallTabell.insert {
        it[DesimaltallTabell.quizOpplysningId] = opplysningId
        it[DesimaltallTabell.svar] = svar
    }
}

private fun hentDesimaltallSvar(it: ResultRow): Double =
    DesimaltallTabell
        .select(DesimaltallTabell.svar)
        .where { DesimaltallTabell.quizOpplysningId eq it[QuizOpplysningTabell.id].value }
        .first()[DesimaltallTabell.svar]

private fun tilBoolskOpplysning(it: ResultRow) =
    QuizOpplysning(
        beskrivendeId = it[QuizOpplysningTabell.beskrivendeId],
        type = Boolsk,
        svar = hentBoolskSvar(it),
        ident = it[QuizOpplysningTabell.ident],
        søknadId = it[QuizOpplysningTabell.søknadId],
    )

private fun lagreBoolskSvar(
    opplysningId: Int,
    svar: Boolean,
) {
    BoolskTabell.insert {
        it[BoolskTabell.quizOpplysningId] = opplysningId
        it[BoolskTabell.svar] = svar
    }
}

private fun hentBoolskSvar(it: ResultRow): Boolean =
    BoolskTabell
        .select(BoolskTabell.svar)
        .where { BoolskTabell.quizOpplysningId eq it[QuizOpplysningTabell.id].value }
        .first()[BoolskTabell.svar]

private fun tilDatoOpplysning(it: ResultRow) =
    QuizOpplysning(
        beskrivendeId = it[QuizOpplysningTabell.beskrivendeId],
        type = Dato,
        svar = hentDatoSvar(it),
        ident = it[QuizOpplysningTabell.ident],
        søknadId = it[QuizOpplysningTabell.søknadId],
    )

private fun lagreDatoSvar(
    opplysningId: Int,
    svar: LocalDate,
) {
    DatoTabell.insert {
        it[DatoTabell.quizOpplysningId] = opplysningId
        it[DatoTabell.svar] = svar
    }
}

private fun hentDatoSvar(it: ResultRow): LocalDate =
    DatoTabell
        .select(DatoTabell.svar)
        .where { DatoTabell.quizOpplysningId eq it[QuizOpplysningTabell.id].value }
        .first()[DatoTabell.svar]

private fun tilFlervalgOpplysning(it: ResultRow) =
    QuizOpplysning(
        beskrivendeId = it[QuizOpplysningTabell.beskrivendeId],
        type = Flervalg,
        svar = hentFlervalgSvar(it),
        ident = it[QuizOpplysningTabell.ident],
        søknadId = it[QuizOpplysningTabell.søknadId],
    )

private fun lagreFlervalgSvar(
    opplysningId: Int,
    svar: List<String>,
) {
    val flervalgId =
        FlervalgTabell
            .insertAndGetId {
                it[FlervalgTabell.quizOpplysningId] = opplysningId
            }.value

    svar.asListOf<String>().forEach { flervalgSvar ->
        FlervalgSvarTabell.insert {
            it[FlervalgSvarTabell.flervalgId] = flervalgId
            it[FlervalgSvarTabell.svar] = flervalgSvar
        }
    }
}

private fun hentFlervalgSvar(it: ResultRow): List<String> {
    val flervalgId =
        FlervalgTabell
            .select(FlervalgTabell.id)
            .where { FlervalgTabell.quizOpplysningId eq it[QuizOpplysningTabell.id].value }
            .first()[FlervalgTabell.id]
            .value

    return FlervalgSvarTabell
        .select(FlervalgSvarTabell.svar)
        .where { FlervalgSvarTabell.flervalgId eq flervalgId }
        .map { it[FlervalgSvarTabell.svar] }
}

private fun tilPeriodeOpplysning(it: ResultRow) =
    QuizOpplysning(
        beskrivendeId = it[QuizOpplysningTabell.beskrivendeId],
        type = Periode,
        svar = hentPeriodeSvar(it),
        ident = it[QuizOpplysningTabell.ident],
        søknadId = it[QuizOpplysningTabell.søknadId],
    )

private fun lagrePeriodeSvar(
    opplysningId: Int,
    svar: PeriodeSvar,
) {
    PeriodeTabell.insert {
        it[PeriodeTabell.quizOpplysningId] = opplysningId
        it[fom] = svar.fom
        it[tom] = svar.tom
    }
}

private fun hentPeriodeSvar(it: ResultRow): PeriodeSvar =
    PeriodeTabell
        .select(PeriodeTabell.fom, PeriodeTabell.tom)
        .where(PeriodeTabell.quizOpplysningId eq it[QuizOpplysningTabell.id].value)
        .map {
            PeriodeSvar(
                fom = it[PeriodeTabell.fom],
                tom = it[PeriodeTabell.tom],
            )
        }.first()

private fun tilArbeidsforholdOpplysning(it: ResultRow) =
    QuizOpplysning(
        beskrivendeId = it[QuizOpplysningTabell.beskrivendeId],
        type = Arbeidsforhold,
        svar = hentArbeidsforholdSvar(it),
        ident = it[QuizOpplysningTabell.ident],
        søknadId = it[QuizOpplysningTabell.søknadId],
    )

private fun lagreArbeidsforholdSvar(
    opplysningId: Int,
    svar: List<ArbeidsforholdSvar>,
) {
    val arbeidsforholdId =
        ArbeidsforholdTabell
            .insertAndGetId {
                it[ArbeidsforholdTabell.quizOpplysningId] = opplysningId
            }.value

    svar.forEach { arbeidsforholdSvar ->
        ArbeidsforholdSvarTabell.insert {
            it[ArbeidsforholdSvarTabell.arbeidsforholdId] = arbeidsforholdId
            it[this.navn] = arbeidsforholdSvar.navn
            it[this.land] = arbeidsforholdSvar.land
            it[this.sluttårsak] = arbeidsforholdSvar.sluttårsak.name
        }
    }
}

fun hentArbeidsforholdSvar(it: ResultRow): List<ArbeidsforholdSvar> {
    val arbeidsforholdId =
        ArbeidsforholdTabell
            .select(ArbeidsforholdTabell.id)
            .where { ArbeidsforholdTabell.quizOpplysningId eq it[QuizOpplysningTabell.id].value }
            .first()[ArbeidsforholdTabell.id]
            .value

    return ArbeidsforholdSvarTabell
        .select(ArbeidsforholdSvarTabell.navn, ArbeidsforholdSvarTabell.land, ArbeidsforholdSvarTabell.sluttårsak)
        .where { ArbeidsforholdSvarTabell.arbeidsforholdId eq arbeidsforholdId }
        .map {
            ArbeidsforholdSvar(
                navn = it[ArbeidsforholdSvarTabell.navn],
                land = it[ArbeidsforholdSvarTabell.land],
                sluttårsak = Sluttårsak.valueOf(it[ArbeidsforholdSvarTabell.sluttårsak]),
            )
        }
}

private fun tilEøsArbeidsforholdOpplysning(it: ResultRow) =
    QuizOpplysning(
        beskrivendeId = it[QuizOpplysningTabell.beskrivendeId],
        type = EøsArbeidsforhold,
        svar = hentEøsArbeidsforholdSvar(it),
        ident = it[QuizOpplysningTabell.ident],
        søknadId = it[QuizOpplysningTabell.søknadId],
    )

private fun lagreEøsArbeidsforholdSvar(
    opplysningId: Int,
    svar: List<EøsArbeidsforholdSvar>,
) {
    val arbeidsforholdId =
        ArbeidsforholdTabell
            .insertAndGetId {
                it[ArbeidsforholdTabell.quizOpplysningId] = opplysningId
            }.value

    svar.forEach { eøsArbeidsforholdSvar ->
        EøsArbeidsforholdSvarTabell.insert {
            it[EøsArbeidsforholdSvarTabell.arbeidsforholdId] = arbeidsforholdId
            it[this.bedriftsnavn] = eøsArbeidsforholdSvar.bedriftsnavn
            it[this.land] = eøsArbeidsforholdSvar.land
            it[this.personnummer] = eøsArbeidsforholdSvar.personnummerIArbeidsland
            it[this.fom] = eøsArbeidsforholdSvar.varighet.fom
            it[this.tom] = eøsArbeidsforholdSvar.varighet.tom
        }
    }
}

fun hentEøsArbeidsforholdSvar(it: ResultRow): List<EøsArbeidsforholdSvar> {
    val arbeidsforholdId =
        ArbeidsforholdTabell
            .select(ArbeidsforholdTabell.id)
            .where { ArbeidsforholdTabell.quizOpplysningId eq it[QuizOpplysningTabell.id].value }
            .first()[ArbeidsforholdTabell.id]
            .value

    return EøsArbeidsforholdSvarTabell
        .select(
            EøsArbeidsforholdSvarTabell.bedriftsnavn,
            EøsArbeidsforholdSvarTabell.land,
            EøsArbeidsforholdSvarTabell.personnummer,
            EøsArbeidsforholdSvarTabell.fom,
            EøsArbeidsforholdSvarTabell.tom,
        ).where { EøsArbeidsforholdSvarTabell.arbeidsforholdId eq arbeidsforholdId }
        .map {
            EøsArbeidsforholdSvar(
                bedriftsnavn = it[EøsArbeidsforholdSvarTabell.bedriftsnavn],
                land = it[EøsArbeidsforholdSvarTabell.land],
                personnummerIArbeidsland = it[EøsArbeidsforholdSvarTabell.personnummer],
                varighet =
                    PeriodeSvar(
                        fom = it[EøsArbeidsforholdSvarTabell.fom],
                        tom = it[EøsArbeidsforholdSvarTabell.tom],
                    ),
            )
        }
}

private fun tilEgenNæringOpplysning(it: ResultRow) =
    QuizOpplysning(
        beskrivendeId = it[QuizOpplysningTabell.beskrivendeId],
        type = EgenNæring,
        svar = hentEgenNæringSvar(it),
        ident = it[QuizOpplysningTabell.ident],
        søknadId = it[QuizOpplysningTabell.søknadId],
    )

private fun lagreEgenNæringSvar(
    opplysningId: Int,
    svar: List<Int>,
) {
    val egenNæringId =
        EgenNæringTabell
            .insertAndGetId {
                it[EgenNæringTabell.quizOpplysningId] = opplysningId
            }.value

    svar.forEach { organisasjonsnummer ->
        EgenNæringSvarTabell.insert {
            it[EgenNæringSvarTabell.egenNæringId] = egenNæringId
            it[EgenNæringSvarTabell.organisasjonsnummer] = organisasjonsnummer
        }
    }
}

private fun hentEgenNæringSvar(it: ResultRow): List<Int> {
    val egenNæringId =
        EgenNæringTabell
            .select(EgenNæringTabell.id)
            .where { EgenNæringTabell.quizOpplysningId eq it[QuizOpplysningTabell.id].value }
            .first()[EgenNæringTabell.id]
            .value

    return EgenNæringSvarTabell
        .select(EgenNæringSvarTabell.organisasjonsnummer)
        .where { EgenNæringSvarTabell.egenNæringId eq egenNæringId }
        .map { it[EgenNæringSvarTabell.organisasjonsnummer] }
}

private fun tilBarnOpplysning(it: ResultRow) =
    QuizOpplysning(
        beskrivendeId = it[QuizOpplysningTabell.beskrivendeId],
        type = Barn,
        svar = hentBarnSvar(it),
        ident = it[QuizOpplysningTabell.ident],
        søknadId = it[QuizOpplysningTabell.søknadId],
    )

private fun hentBarnSvar(it: ResultRow): List<BarnSvar> {
    val barnId =
        BarnTabell
            .select(BarnTabell.id)
            .where { BarnTabell.quizOpplysningId eq it[QuizOpplysningTabell.id].value }
            .first()[BarnTabell.id]
            .value

    return BarnSvarTabell
        .select(
            BarnSvarTabell.barnSvarId,
            BarnSvarTabell.fornavnMellomnavn,
            BarnSvarTabell.etternavn,
            BarnSvarTabell.fødselsdato,
            BarnSvarTabell.statsborgerskap,
            BarnSvarTabell.forsørgerBarnet,
            BarnSvarTabell.fraRegister,
            BarnSvarTabell.kvalifisererTilBarnetillegg,
            BarnSvarTabell.barnetilleggFom,
            BarnSvarTabell.barnetilleggTom,
            BarnSvarTabell.endretAv,
            BarnSvarTabell.begrunnelse,
        ).where { BarnSvarTabell.barnId eq barnId }
        .map {
            BarnSvar(
                barnSvarId = it[BarnSvarTabell.barnSvarId],
                fornavnOgMellomnavn = it[BarnSvarTabell.fornavnMellomnavn],
                etternavn = it[BarnSvarTabell.etternavn],
                fødselsdato = it[BarnSvarTabell.fødselsdato],
                statsborgerskap = it[BarnSvarTabell.statsborgerskap],
                forsørgerBarnet = it[BarnSvarTabell.forsørgerBarnet],
                fraRegister = it[BarnSvarTabell.fraRegister],
                kvalifisererTilBarnetillegg = it[BarnSvarTabell.kvalifisererTilBarnetillegg],
                barnetilleggFom = it[BarnSvarTabell.barnetilleggFom],
                barnetilleggTom = it[BarnSvarTabell.barnetilleggTom],
                endretAv = it[BarnSvarTabell.endretAv],
                begrunnelse = it[BarnSvarTabell.begrunnelse],
            )
        }
}

private fun lagreBarnSvar(
    opplysningId: Int,
    svar: List<BarnSvar>,
) {
    val barnId =
        BarnTabell
            .insertAndGetId {
                it[BarnTabell.quizOpplysningId] = opplysningId
            }.value

    svar.forEach { barn ->
        BarnSvarTabell.insert {
            it[barnSvarId] = barn.barnSvarId
            it[BarnSvarTabell.barnId] = barnId
            it[fornavnMellomnavn] = barn.fornavnOgMellomnavn
            it[etternavn] = barn.etternavn
            it[fødselsdato] = barn.fødselsdato
            it[statsborgerskap] = barn.statsborgerskap
            it[forsørgerBarnet] = barn.forsørgerBarnet
            it[fraRegister] = barn.fraRegister
            it[kvalifisererTilBarnetillegg] = barn.kvalifisererTilBarnetillegg
            it[barnetilleggFom] = barn.barnetilleggFom
            it[barnetilleggTom] = barn.barnetilleggTom
        }
    }
}
