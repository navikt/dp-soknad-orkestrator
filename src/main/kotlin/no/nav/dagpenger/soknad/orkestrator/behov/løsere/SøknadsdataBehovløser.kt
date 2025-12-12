package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.ktor.server.plugins.NotFoundException
import no.nav.dagpenger.soknad.orkestrator.behov.Behovløser
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.Søknadsdata
import no.nav.dagpenger.soknad.orkestrator.behov.Behovmelding
import no.nav.dagpenger.soknad.orkestrator.behov.FellesBehovløserLøsninger
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.asListOf
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.ArbeidsforholdSvar
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Sluttårsak
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonRepository
import java.time.LocalDate
import java.util.UUID

class SøknadsdataBehovløser(
    rapidsConnection: RapidsConnection,
    opplysningRepository: QuizOpplysningRepository,
    søknadRepository: SøknadRepository,
    seksjonRepository: SeksjonRepository,
    fellesBehovløserLøsninger: FellesBehovløserLøsninger,
) : Behovløser(rapidsConnection, opplysningRepository, søknadRepository, seksjonRepository, fellesBehovløserLøsninger) {
    override val behov = Søknadsdata.name
    override val beskrivendeId = "behov.søknadsdata"

    override fun løs(behovmelding: Behovmelding) {
        if (fellesBehovløserLøsninger == null) return

        val journalpostId =
            behovmelding.innkommendePacket.get("journalpostId").asText() ?: throw IllegalStateException(
                "Mangler journalpostId i behov for søknadsdata for søknadId: ${behovmelding.søknadId}",
            )

        val søknadId =
            søknadRepository.hentSøknadIdFraJournalPostId(journalpostId, behovmelding.ident)

        val eøsBostedsland = eøsBostedsland(behovmelding.ident, søknadId)
        val eøsArbeidsforhold =
            fellesBehovløserLøsninger.harSøkerenHattArbeidsforholdIEøs(
                "faktum.eos-arbeid-siste-36-mnd",
                behovmelding.ident,
                søknadId,
            )
        val avsluttetArbeidsforhold = finnAvsluttedeArbeidsforhold(behovmelding.ident, søknadId)
        val avtjentVerneplikt = avtjentVerneplikt(behovmelding.ident, søknadId)
        val harBarn = harSøkerBarn(behovmelding.ident, søknadId)
        val harAndreYtelser = harAndreYtelser(behovmelding.ident, søknadId)
        val ønskerDagpengerFraDato =
            fellesBehovløserLøsninger.ønskerDagpengerFraDato(
                ident = behovmelding.ident,
                søknadId = søknadId,
            )
        val reellArbeidssøker =
            erReellArbeidssøker(
                behovmelding.ident,
                søknadId,
            )

        val søknadsdataResultat =
            SøknadsdataResultType(
                eøsBostedsland = eøsBostedsland,
                eøsArbeidsforhold = eøsArbeidsforhold,
                avtjentVerneplikt = avtjentVerneplikt,
                avsluttetArbeidsforhold = avsluttetArbeidsforhold,
                harBarn = harBarn,
                harAndreYtelser = harAndreYtelser,
                ønskerDagpengerFraDato = ønskerDagpengerFraDato,
                søknadId = søknadId.toString(),
                reellArbeidssøker = reellArbeidssøker,
            )

        val søknadsdataJson = objectMapper.writeValueAsString(søknadsdataResultat)
        return publiserLøsning(behovmelding, søknadsdataJson)
    }

    private fun erReellArbeidssøker(
        ident: String,
        søknadId: UUID,
    ): ReellArbeidssøker {
        val seksjonsvar =
            seksjonRepository.hentSeksjonsvarEllerKastException(
                ident,
                søknadId,
                "reell-arbeidssoker",
            )

        val reellArbeidssøkerSeksjon = objectMapper.readTree(seksjonsvar)

        val kanDuTaAlleTyperArbeid =
            reellArbeidssøkerSeksjon.finnOpplysning("kanDuTaAlleTyperArbeid").asText() == "ja"

        val kanDuJobbeIHeleNorge =
            reellArbeidssøkerSeksjon.finnOpplysning("kanDuJobbeIHeleNorge").asText() == "ja"

        val kanDuJobbeBådeHeltidOgDeltid =
            reellArbeidssøkerSeksjon.finnOpplysning("kanDuJobbeBådeHeltidOgDeltid").asText() == "ja"

        val erDuVilligTilÅBytteYrkeEllerGåNedILønn =
            reellArbeidssøkerSeksjon.finnOpplysning("erDuVilligTilÅBytteYrkeEllerGåNedILønn").asText() == "ja"

        return ReellArbeidssøker(
            helse = kanDuTaAlleTyperArbeid,
            geografi = kanDuJobbeIHeleNorge,
            deltid = kanDuJobbeBådeHeltidOgDeltid,
            yrke = erDuVilligTilÅBytteYrkeEllerGåNedILønn,
        )
    }

    private fun harAndreYtelser(
        ident: String,
        søknadId: UUID,
    ): Boolean {
        val seksjonsvar =
            seksjonRepository.hentSeksjonsvar(
                søknadId,
                ident,
                "annen-pengestotte",
            ) ?: throw NotFoundException("Finner ikke seksjon annen-pengestotte for søknad $søknadId")

        val annenPengestøtteSeksjon = objectMapper.readTree(seksjonsvar)

        val mottarDuEllerHarDuSøktOmPengestøtteFraAndreEnnNav: Boolean =
            annenPengestøtteSeksjon.finnOpplysning("mottarDuEllerHarDuSøktOmPengestøtteFraAndreEnnNav").asText() == "ja"

        val harMottattEllerSøktOmPengestøtteFraAndreEøsLand =
            annenPengestøtteSeksjon.finnOpplysning("harMottattEllerSøktOmPengestøtteFraAndreEøsLand").asText() == "ja"

        val fårEllerKommerTilÅFåLønnEllerAndreGoderFraTidligereArbeidsgiver =
            annenPengestøtteSeksjon.finnOpplysning("fårEllerKommerTilÅFåLønnEllerAndreGoderFraTidligereArbeidsgiver").asText() == "ja"

        return mottarDuEllerHarDuSøktOmPengestøtteFraAndreEnnNav ||
            harMottattEllerSøktOmPengestøtteFraAndreEøsLand ||
            fårEllerKommerTilÅFåLønnEllerAndreGoderFraTidligereArbeidsgiver
    }

    private fun harSøkerBarn(
        ident: String,
        søknadId: UUID,
    ): Boolean {
        val seksjonsvar =
            seksjonRepository.hentSeksjonsvarEllerKastException(
                ident,
                søknadId,
                "barnetillegg",
            )

        val pdlBarn =
            objectMapper.readTree(seksjonsvar).let { seksjonJson ->
                seksjonJson.findPath("barnFraPdl")?.toList() ?: emptyList()
            }

        val egneBarn =
            objectMapper.readTree(seksjonsvar).let { seksjonJson ->
                seksjonJson.findPath("barnLagtManuelt")?.toList() ?: emptyList()
            }

        return !(pdlBarn.isEmpty() && egneBarn.isEmpty())
    }

    fun avtjentVerneplikt(
        ident: String,
        søknadId: UUID,
    ): Boolean =
        fellesBehovløserLøsninger!!.harSøkerenAvtjentVerneplikt(
            behov,
            "faktum.avtjent-militaer-sivilforsvar-tjeneste-siste-12-mnd",
            ident,
            søknadId,
        )

    fun finnAvsluttedeArbeidsforhold(
        ident: String,
        søknadId: UUID,
    ): List<AvsluttedeArbeidsforhold> {
        val arbeidsforholdOpplysning = opplysningRepository.hent("faktum.arbeidsforhold", ident, søknadId)

        if (arbeidsforholdOpplysning != null) {
            arbeidsforholdOpplysning.svar.asListOf<ArbeidsforholdSvar>().let { arbeidsforholdListe ->
                return arbeidsforholdListe.map {
                    AvsluttedeArbeidsforhold(
                        sluttårsak = finnSluttÅrsakForQuiz(it.sluttårsak),
                        fiskeforedling = it.sluttårsak == Sluttårsak.PERMITTERT_FISKEFOREDLING,
                        land = it.land,
                    )
                }
            }
        }

        val seksjonsSvar =
            try {
                seksjonRepository.hentSeksjonsvarEllerKastException(
                    ident,
                    søknadId,
                    "arbeidsforhold",
                )
            } catch (e: IllegalStateException) {
                return emptyList()
            }

        objectMapper.readTree(seksjonsSvar).let { seksjonsJson ->
            seksjonsJson.findPath("registrerteArbeidsforhold")?.let {
                if (!it.isMissingOrNull()) {
                    return it.map {
                        AvsluttedeArbeidsforhold(
                            sluttårsak = it.sluttårsak(),
                            fiskeforedling = it.fiskForedling(),
                            land = it.findPath("hvilketLandJobbetDuI").asText(),
                        )
                    }
                }
            }
        }

        return emptyList()
    }

    private fun JsonNode.finnOpplysning(navn: String): JsonNode =
        this.findPath(navn) ?: throw NoSuchElementException("Finner ikke opplysning med navn: $navn")

    private fun JsonNode.sluttårsak(): Sluttårsak =
        this.findPath("hvordanHarDetteArbeidsforholdetEndretSeg").asText().let {
            mapSluttÅrsak(it)
        }

    private fun mapSluttÅrsak(string: String?): Sluttårsak =
        when (string) {
            "arbeidsforholdetErIkkeEndret" -> Sluttårsak.IKKE_ENDRET
            "jegHarFåttAvskjed" -> Sluttårsak.AVSKJEDIGET
            "arbeidsgiverenMinHarSagtMegOpp" -> Sluttårsak.SAGT_OPP_AV_ARBEIDSGIVER
            "arbeidsgiverErKonkurs" -> Sluttårsak.ARBEIDSGIVER_KONKURS
            "kontraktenErUtgått" -> Sluttårsak.KONTRAKT_UTGAATT
            "jegHarSagtOppSelv" -> Sluttårsak.SAGT_OPP_SELV
            "arbeidstidenErRedusert" -> Sluttårsak.REDUSERT_ARBEIDSTID
            "jegErPermitert" -> Sluttårsak.PERMITTERT
            else -> throw IllegalArgumentException("Ukjent sluttårsak: $string")
        }

    private fun JsonNode.fiskForedling(): Boolean =
        this.finnOpplysning("permittertErDuPermittertFraFiskeforedlingsEllerFiskeoljeindustrien").asText() == "ja"

    private fun finnSluttÅrsakForQuiz(sluttårsak: Sluttårsak): Sluttårsak {
        if (sluttårsak == Sluttårsak.PERMITTERT_FISKEFOREDLING) {
            return Sluttårsak.PERMITTERT
        }
        return sluttårsak
    }

    fun eøsBostedsland(
        ident: String,
        søknadId: UUID,
    ): Boolean {
        val svarPåBehov =
            opplysningRepository.hent("faktum.hvilket-land-bor-du-i", ident, søknadId)

        if (svarPåBehov != null) {
            var bostedsland = svarPåBehov.svar as String
            return eøsLandOgSveits.contains(bostedsland)
        }

        val seksjonsSvar =
            seksjonRepository.hentSeksjonsvar(
                søknadId,
                ident,
                "personalia",
            )

        val personaliaSeksjonsData = objectMapper.readTree(seksjonsSvar)
        val borINorge = personaliaSeksjonsData.finnOpplysning("folkeregistrertAdresseErNorgeStemmerDet").asText()
        if (borINorge == "ja") return false

        val bostedsland = personaliaSeksjonsData.finnOpplysning("bostedsland").asText()
        return eøsLandOgSveits.contains(bostedsland)
    }

    val eøsLandOgSveits =
        listOf(
            "BEL",
            "BGR",
            "DNK",
            "EST",
            "FIN",
            "FRA",
            "GRC",
            "IRL",
            "ISL",
            "ITA",
            "HRV",
            "CYP",
            "LVA",
            "LIE",
            "LTU",
            "LUX",
            "MLT",
            "NLD",
            "POL",
            "PRT",
            "ROU",
            "SVK",
            "SVN",
            "ESP",
            "CHE",
            "SWE",
            "CZE",
            "DEU",
            "HUN",
            "AUT",
        )
}

data class SøknadsdataResultType(
    val eøsBostedsland: Boolean,
    val eøsArbeidsforhold: Boolean,
    val avtjentVerneplikt: Boolean,
    val avsluttetArbeidsforhold: List<AvsluttedeArbeidsforhold>,
    val harBarn: Boolean,
    val harAndreYtelser: Boolean,
    val ønskerDagpengerFraDato: LocalDate,
    val søknadId: String,
    val reellArbeidssøker: ReellArbeidssøker,
)

data class AvsluttedeArbeidsforhold(
    val sluttårsak: Sluttårsak,
    val fiskeforedling: Boolean,
    val land: String,
)

data class ReellArbeidssøker(
    val helse: Boolean,
    val geografi: Boolean,
    val deltid: Boolean,
    val yrke: Boolean,
)
