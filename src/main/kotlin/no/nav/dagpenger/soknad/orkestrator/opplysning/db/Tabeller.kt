import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
import java.util.UUID

object OpplysningTabell : IntIdTable("opplysning") {
    val opprettet: Column<LocalDateTime> = datetime("opprettet").default(LocalDateTime.now())
    val beskrivendeId: Column<String> = varchar("beskrivende_id", 255)
    val type: Column<String> = text("type")
    val ident: Column<String> = varchar("ident", 11)
    val søknadId: Column<UUID> = uuid("soknads_id")
}

object TekstTabell : IntIdTable("tekst") {
    val opplysningId: Column<Int> = integer("opplysning_id").references(OpplysningTabell.id)
    val svar = text("svar")
}

object HeltallTabell : IntIdTable("heltall") {
    val opplysningId: Column<Int> = integer("opplysning_id").references(OpplysningTabell.id)
    val svar = integer("svar")
}

object DesimaltallTabell : IntIdTable("desimaltall") {
    val opplysningId: Column<Int> = integer("opplysning_id").references(OpplysningTabell.id)
    val svar = double("svar")
}

object BoolskTabell : IntIdTable("boolsk") {
    val opplysningId: Column<Int> = integer("opplysning_id").references(OpplysningTabell.id)
    val svar = bool("svar")
}

object DatoTabell : IntIdTable("dato") {
    val opplysningId: Column<Int> = integer("opplysning_id").references(OpplysningTabell.id)
    val svar = date("svar")
}

object FlervalgTabell : IntIdTable("flervalg") {
    val opplysningId: Column<Int> = integer("opplysning_id").references(OpplysningTabell.id)
}

object FlervalgSvarTabell : IntIdTable("flervalg_svar") {
    val flervalgId: Column<Int> = integer("flervalg_id").references(FlervalgTabell.id)
    val svar = text("svar")
}

object PeriodeTabell : IntIdTable("periode") {
    val opplysningId: Column<Int> = integer("opplysning_id").references(OpplysningTabell.id)
    val fom = date("fom")
    val tom = date("tom").nullable()
}

object ArbeidsforholdTabell : IntIdTable("arbeidsforhold") {
    val opplysningId: Column<Int> = integer("opplysning_id").references(OpplysningTabell.id)
}

object ArbeidsforholdSvarTabell : IntIdTable("arbeidsforhold_svar") {
    val arbeidsforholdId: Column<Int> = integer("arbeidsforhold_id").references(ArbeidsforholdTabell.id)
    val navnSvarId: Column<Int> = integer("navn_svar_id").references(TekstTabell.id)
    val landSvarId: Column<Int> = integer("land_svar_id").references(TekstTabell.id)
}

object EøsArbeidsforholdSvarTabell : IntIdTable("eøs_arbeidsforhold_svar") {
    val arbeidsforholdId: Column<Int> = integer("arbeidsforhold_id").references(ArbeidsforholdTabell.id)
    val bedriftnavnSvarId: Column<Int> = integer("bedrift_navn_svar_id").references(TekstTabell.id)
    val landSvarId: Column<Int> = integer("land_svar_id").references(TekstTabell.id)
    val personnummerSvarId: Column<Int> = integer("personnummer_svar_id").references(TekstTabell.id)
    val varighetSvarId: Column<Int> = integer("varighet_svar_id").references(PeriodeTabell.id)
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
    val fornavnMellomnavnId: Column<Int> = integer("fornavn_mellomnavn_id").references(TekstTabell.id)
    val etternavnId: Column<Int> = integer("etternavn_id").references(TekstTabell.id)
    val fødselsdatoId: Column<Int> = integer("fødselsdato_id").references(DatoTabell.id)
    val statsborgerskapId: Column<Int> = integer("statsborgerskap_id").references(TekstTabell.id)
    val forsørgerId: Column<Int> = integer("forsørger_barnet_id").references(BoolskTabell.id)
    val fraRegister = bool("fra_register")
}
