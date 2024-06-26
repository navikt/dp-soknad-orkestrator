package no.nav.dagpenger.soknad.orkestrator.opplysning.db

import ArbeidsforholdSvarTabell
import BarnSvarTabell
import BoolskTabell
import DatoTabell
import DesimaltallTabell
import EgenNæringSvarTabell
import EgenNæringTabell
import EøsArbeidsforholdSvarTabell
import FlervalgSvarTabell
import HeltallTabell
import OpplysningTabell
import TekstTabell
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysning
import no.nav.dagpenger.soknad.orkestrator.opplysning.asListOf
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.Arbeidsforhold
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.ArbeidsforholdSvar
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.Barn
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.BarnSvar
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.Boolsk
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.Dato
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.Desimaltall
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.EgenNæring
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.EøsArbeidsforhold
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.EøsArbeidsforholdSvar
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.Flervalg
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.Heltall
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.Periode
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.PeriodeSvar
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.Sluttårsak
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.Tekst
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
import java.time.LocalDate
import java.util.UUID
import javax.sql.DataSource

class OpplysningRepositoryPostgres(dataSource: DataSource) : OpplysningRepository {
    val database = Database.connect(dataSource)

    override fun lagre(opplysning: Opplysning<*>) {
        transaction {
            if (!opplysningEksisterer(opplysning)) {
                val opplysningId = OpplysningTabell.insertAndGetId(opplysning)

                when (opplysning.type) {
                    Tekst -> lagreTekstSvar(opplysningId, opplysning.svar as String)
                    Heltall -> lagreHeltallSvar(opplysningId, opplysning.svar as Int)
                    Desimaltall -> lagreDesimaltallSvar(opplysningId, opplysning.svar as Double)
                    Boolsk -> lagreBoolskSvar(opplysningId, opplysning.svar as Boolean)
                    Dato -> lagreDatoSvar(opplysningId, opplysning.svar as LocalDate)
                    Flervalg -> lagreFlervalgSvar(opplysningId, opplysning.svar.asListOf<String>())
                    Periode -> lagrePeriodeSvar(opplysningId, opplysning.svar as PeriodeSvar)
                    Arbeidsforhold ->
                        lagreArbeidsforholdSvar(
                            opplysningId,
                            opplysning.svar.asListOf<ArbeidsforholdSvar>(),
                        )

                    EøsArbeidsforhold ->
                        lagreEøsArbeidsforholdSvar(
                            opplysningId,
                            opplysning.svar.asListOf<EøsArbeidsforholdSvar>(),
                        )

                    EgenNæring -> lagreEgenNæringSvar(opplysningId, opplysning.svar.asListOf<Int>())
                    Barn -> lagreBarnSvar(opplysningId, opplysning.svar.asListOf<BarnSvar>())
                }
            }
        }
    }

    override fun hent(
        beskrivendeId: String,
        ident: String,
        søknadId: UUID,
    ): Opplysning<*>? {
        return transaction {
            OpplysningTabell
                .selectAll()
                .somMatcher(beskrivendeId, ident, søknadId)
                .map(tilOpplysning())
                .firstOrNull()
        }
    }

    override fun hentAlle(søknadId: UUID): List<Opplysning<*>> {
        return transaction {
            OpplysningTabell
                .selectAll()
                .where { OpplysningTabell.søknadId eq søknadId }
                .map(tilOpplysning())
        }
    }

    override fun slett(søknadId: UUID) {
        transaction {
            OpplysningTabell.deleteWhere { OpplysningTabell.søknadId eq søknadId }
        }
    }
}

private fun opplysningEksisterer(opplysning: Opplysning<*>): Boolean =
    OpplysningTabell.selectAll().somMatcher(
        opplysning.beskrivendeId,
        opplysning.ident,
        opplysning.søknadId,
    ).any()

fun OpplysningTabell.insertAndGetId(opplysning: Opplysning<*>) =
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
        OpplysningTabell.beskrivendeId eq beskrivendeId and
            (OpplysningTabell.ident eq ident) and
            (OpplysningTabell.søknadId eq søknadId)
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
            "EgenNæring" -> tilEgenNæringOpplysning(it)
            "Barn" -> tilBarnOpplysning(it)
            else -> throw IllegalArgumentException("Ukjent datatype: ${it[OpplysningTabell.type]}")
        }
    }

private fun tilTekstOpplysning(it: ResultRow) =
    Opplysning(
        beskrivendeId = it[OpplysningTabell.beskrivendeId],
        type = Tekst,
        svar = hentTekstSvar(it),
        ident = it[OpplysningTabell.ident],
        søknadId = it[OpplysningTabell.søknadId],
    )

private fun lagreTekstSvar(
    opplysningId: Int,
    svar: String,
) {
    TekstTabell.insert {
        it[TekstTabell.opplysningId] = opplysningId
        it[TekstTabell.svar] = svar
    }
}

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
        søknadId = it[OpplysningTabell.søknadId],
    )

private fun lagreHeltallSvar(
    opplysningId: Int,
    svar: Int,
) {
    HeltallTabell.insert {
        it[HeltallTabell.opplysningId] = opplysningId
        it[HeltallTabell.svar] = svar
    }
}

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
        søknadId = it[OpplysningTabell.søknadId],
    )

private fun lagreDesimaltallSvar(
    opplysningId: Int,
    svar: Double,
) {
    DesimaltallTabell.insert {
        it[DesimaltallTabell.opplysningId] = opplysningId
        it[DesimaltallTabell.svar] = svar
    }
}

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
        søknadId = it[OpplysningTabell.søknadId],
    )

private fun lagreBoolskSvar(
    opplysningId: Int,
    svar: Boolean,
) {
    BoolskTabell.insert {
        it[BoolskTabell.opplysningId] = opplysningId
        it[BoolskTabell.svar] = svar
    }
}

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
        søknadId = it[OpplysningTabell.søknadId],
    )

private fun lagreDatoSvar(
    opplysningId: Int,
    svar: LocalDate,
) {
    DatoTabell.insert {
        it[DatoTabell.opplysningId] = opplysningId
        it[DatoTabell.svar] = svar
    }
}

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
        søknadId = it[OpplysningTabell.søknadId],
    )

private fun lagreFlervalgSvar(
    opplysningId: Int,
    svar: List<String>,
) {
    val flervalgId =
        FlervalgTabell.insertAndGetId {
            it[FlervalgTabell.opplysningId] = opplysningId
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
        søknadId = it[OpplysningTabell.søknadId],
    )

private fun lagrePeriodeSvar(
    opplysningId: Int,
    svar: PeriodeSvar,
) {
    PeriodeTabell.insert {
        it[PeriodeTabell.opplysningId] = opplysningId
        it[fom] = svar.fom
        it[tom] = svar.tom
    }
}

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
        søknadId = it[OpplysningTabell.søknadId],
    )

private fun lagreArbeidsforholdSvar(
    opplysningId: Int,
    svar: List<ArbeidsforholdSvar>,
) {
    val arbeidsforholdId =
        ArbeidsforholdTabell.insertAndGetId {
            it[ArbeidsforholdTabell.opplysningId] = opplysningId
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
            .where { ArbeidsforholdTabell.opplysningId eq it[OpplysningTabell.id].value }
            .first()[ArbeidsforholdTabell.id].value

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
    Opplysning(
        beskrivendeId = it[OpplysningTabell.beskrivendeId],
        type = EøsArbeidsforhold,
        svar = hentEøsArbeidsforholdSvar(it),
        ident = it[OpplysningTabell.ident],
        søknadId = it[OpplysningTabell.søknadId],
    )

private fun lagreEøsArbeidsforholdSvar(
    opplysningId: Int,
    svar: List<EøsArbeidsforholdSvar>,
) {
    val arbeidsforholdId =
        ArbeidsforholdTabell.insertAndGetId {
            it[ArbeidsforholdTabell.opplysningId] = opplysningId
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
            .where { ArbeidsforholdTabell.opplysningId eq it[OpplysningTabell.id].value }
            .first()[ArbeidsforholdTabell.id].value

    return EøsArbeidsforholdSvarTabell
        .select(
            EøsArbeidsforholdSvarTabell.bedriftsnavn,
            EøsArbeidsforholdSvarTabell.land,
            EøsArbeidsforholdSvarTabell.personnummer,
            EøsArbeidsforholdSvarTabell.fom,
            EøsArbeidsforholdSvarTabell.tom,
        )
        .where { EøsArbeidsforholdSvarTabell.arbeidsforholdId eq arbeidsforholdId }
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
    Opplysning(
        beskrivendeId = it[OpplysningTabell.beskrivendeId],
        type = EgenNæring,
        svar = hentEgenNæringSvar(it),
        ident = it[OpplysningTabell.ident],
        søknadId = it[OpplysningTabell.søknadId],
    )

private fun lagreEgenNæringSvar(
    opplysningId: Int,
    svar: List<Int>,
) {
    val egenNæringId =
        EgenNæringTabell.insertAndGetId {
            it[EgenNæringTabell.opplysningId] = opplysningId
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
            .where { EgenNæringTabell.opplysningId eq it[OpplysningTabell.id].value }.first()[EgenNæringTabell.id].value

    return EgenNæringSvarTabell
        .select(EgenNæringSvarTabell.organisasjonsnummer)
        .where { EgenNæringSvarTabell.egenNæringId eq egenNæringId }
        .map { it[EgenNæringSvarTabell.organisasjonsnummer] }
}

private fun tilBarnOpplysning(it: ResultRow) =
    Opplysning(
        beskrivendeId = it[OpplysningTabell.beskrivendeId],
        type = Barn,
        svar = hentBarnSvar(it),
        ident = it[OpplysningTabell.ident],
        søknadId = it[OpplysningTabell.søknadId],
    )

private fun hentBarnSvar(it: ResultRow): List<BarnSvar> {
    val barnId =
        BarnTabell
            .select(BarnTabell.id)
            .where { BarnTabell.opplysningId eq it[OpplysningTabell.id].value }
            .first()[BarnTabell.id].value

    return BarnSvarTabell
        .select(
            BarnSvarTabell.fornavnMellomnavn,
            BarnSvarTabell.etternavn,
            BarnSvarTabell.fødselsdato,
            BarnSvarTabell.statsborgerskap,
            BarnSvarTabell.forsørgerBarnet,
            BarnSvarTabell.fraRegister,
        )
        .where { BarnSvarTabell.barnId eq barnId }
        .map {
            BarnSvar(
                fornavnOgMellomnavn = it[BarnSvarTabell.fornavnMellomnavn],
                etternavn = it[BarnSvarTabell.etternavn],
                fødselsdato = it[BarnSvarTabell.fødselsdato],
                statsborgerskap = it[BarnSvarTabell.statsborgerskap],
                forsørgerBarnet = it[BarnSvarTabell.forsørgerBarnet],
                fraRegister = it[BarnSvarTabell.fraRegister],
            )
        }
}

private fun lagreBarnSvar(
    opplysningId: Int,
    svar: List<BarnSvar>,
) {
    val barnId =
        BarnTabell.insertAndGetId {
            it[BarnTabell.opplysningId] = opplysningId
        }.value

    svar.forEach { barn ->
        BarnSvarTabell.insert {
            it[BarnSvarTabell.barnId] = barnId
            it[fornavnMellomnavn] = barn.fornavnOgMellomnavn
            it[etternavn] = barn.etternavn
            it[fødselsdato] = barn.fødselsdato
            it[statsborgerskap] = barn.statsborgerskap
            it[forsørgerBarnet] = barn.forsørgerBarnet
            it[fraRegister] = barn.fraRegister
        }
    }
}
