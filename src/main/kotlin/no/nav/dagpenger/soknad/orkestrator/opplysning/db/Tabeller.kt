import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

object SøknadTabell : IntIdTable("soknad") {
    val opprettet: Column<LocalDateTime> = datetime("opprettet").default(LocalDateTime.now())
    val søknadId: Column<UUID> = uuid("soknad_id")
    val ident: Column<String> = varchar("ident", 11)
}

object OpplysningTabell : IntIdTable("opplysning") {
    val opprettet: Column<LocalDateTime> = datetime("opprettet").default(LocalDateTime.now())
    val beskrivendeId: Column<String> = varchar("beskrivende_id", 255)
    val type: Column<String> = text("type")
    val ident: Column<String> = varchar("ident", 11)
    val søknadId: Column<UUID> = uuid("soknad_id")
}

object TekstTabell : IntIdTable("tekst") {
    val opplysningId: Column<Int> = integer("opplysning_id").references(OpplysningTabell.id)
    val svar: Column<String> = text("svar")
}

object HeltallTabell : IntIdTable("heltall") {
    val opplysningId: Column<Int> = integer("opplysning_id").references(OpplysningTabell.id)
    val svar: Column<Int> = integer("svar")
}

object DesimaltallTabell : IntIdTable("desimaltall") {
    val opplysningId: Column<Int> = integer("opplysning_id").references(OpplysningTabell.id)
    val svar: Column<Double> = double("svar")
}

object BoolskTabell : IntIdTable("boolsk") {
    val opplysningId: Column<Int> = integer("opplysning_id").references(OpplysningTabell.id)
    val svar: Column<Boolean> = bool("svar")
}

object DatoTabell : IntIdTable("dato") {
    val opplysningId: Column<Int> = integer("opplysning_id").references(OpplysningTabell.id)
    val svar: Column<LocalDate> = date("svar")
}

object FlervalgTabell : IntIdTable("flervalg") {
    val opplysningId: Column<Int> = integer("opplysning_id").references(OpplysningTabell.id)
}

object FlervalgSvarTabell : IntIdTable("flervalg_svar") {
    val flervalgId: Column<Int> = integer("flervalg_id").references(FlervalgTabell.id)
    val svar: Column<String> = text("svar")
}

object PeriodeTabell : IntIdTable("periode") {
    val opplysningId: Column<Int> = integer("opplysning_id").references(OpplysningTabell.id)
    val fom: Column<LocalDate> = date("fom")
    val tom: Column<LocalDate?> = date("tom").nullable()
}

object ArbeidsforholdTabell : IntIdTable("arbeidsforhold") {
    val opplysningId: Column<Int> = integer("opplysning_id").references(OpplysningTabell.id)
}

object ArbeidsforholdSvarTabell : IntIdTable("arbeidsforhold_svar") {
    val arbeidsforholdId: Column<Int> = integer("arbeidsforhold_id").references(ArbeidsforholdTabell.id)
    val navn: Column<String> = text("navn")
    val land: Column<String> = text("land")
    val sluttårsak: Column<String> = text("sluttårsak")
}

object EøsArbeidsforholdSvarTabell : IntIdTable("eøs_arbeidsforhold_svar") {
    val arbeidsforholdId: Column<Int> = integer("arbeidsforhold_id").references(ArbeidsforholdTabell.id)
    val bedriftsnavn: Column<String> = text("bedriftsnavn")
    val land: Column<String> = text("land")
    val personnummer: Column<String> = text("personnummer")
    val fom: Column<LocalDate> = date("fom")
    val tom: Column<LocalDate?> = date("tom").nullable()
}

object EgenNæringTabell : IntIdTable("egen_næring") {
    val opplysningId: Column<Int> = integer("opplysning_id").references(OpplysningTabell.id)
}

object EgenNæringSvarTabell : IntIdTable("egen_næring_svar") {
    val egenNæringId: Column<Int> = integer("egen_næring_id").references(EgenNæringTabell.id)
    val organisasjonsnummer = integer("organisasjonsnummer")
}

object BarnTabell : IntIdTable("barn") {
    val opplysningId: Column<Int> = integer("opplysning_id").references(OpplysningTabell.id)
}

object BarnSvarTabell : IntIdTable("barn_svar") {
    val barnId: Column<Int> = integer("barn_id").references(BarnTabell.id)
    val fornavnMellomnavn: Column<String> = text("fornavn_mellomnavn")
    val etternavn: Column<String> = text("etternavn")
    val fødselsdato: Column<LocalDate> = date("fødselsdato")
    val statsborgerskap: Column<String> = text("statsborgerskap")
    val forsørgerBarnet: Column<Boolean> = bool("forsørger_barnet")
    val fraRegister = bool("fra_register")
}
