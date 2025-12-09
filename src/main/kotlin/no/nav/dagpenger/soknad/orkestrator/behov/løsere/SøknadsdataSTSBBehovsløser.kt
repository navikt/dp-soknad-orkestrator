package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.ktor.server.plugins.NotFoundException
import no.nav.dagpenger.soknad.orkestrator.behov.Behovløser
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.SøknadsdataSTSB
import no.nav.dagpenger.soknad.orkestrator.behov.Behovmelding
import no.nav.dagpenger.soknad.orkestrator.behov.CommonBehovsløser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.SøknadsdataSTSBBehovsløser.AvsluttedeArbeidsforhold.Sluttårsaken
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonRepository
import java.time.LocalDate
import java.util.UUID

class SøknadsdataSTSBBehovsløser(
    rapidsConnection: RapidsConnection,
    opplysningRepository: QuizOpplysningRepository,
    søknadRepository: SøknadRepository,
    seksjonRepository: SeksjonRepository,
    commonBehovsløser: CommonBehovsløser,
) : Behovløser(rapidsConnection, opplysningRepository, søknadRepository, seksjonRepository, commonBehovsløser) {
    override val behov = SøknadsdataSTSB.name
    override val beskrivendeId = "behov.søknadsdata-stsb"

    override fun løs(behovmelding: Behovmelding) {
        if (commonBehovsløser == null) return

        val journalpostId =
            behovmelding.innkommendePacket.get("journalpostId").asText() ?: throw IllegalStateException(
                "Mangler journalpostId i behov for søknadsdata STSB for søknadId: ${behovmelding.søknadId}",
            )

        val søknadId =
            søknadRepository.hentSøknadIdFraJournalPostId(journalpostId, behovmelding.ident)
        // Personalia
        val eøsBostedsland = eøsBostedsland(behovmelding.ident, søknadId)

        // arbeidsforhold
        val eøsArbeidsforhold = commonBehovsløser.harSøkerenHattArbeidsforholdIEøs(beskrivendeId, behovmelding.ident, søknadId)
        val avsluttetArbeidsforhold = finnAvsluttedeArbeidsforhold(behovmelding.ident, søknadId)

        // verneplikt
        val avtjentVerneplikt = avtjentVerneplikt(behovmelding.ident, søknadId)

        // barnetillegg
        val harBarn = harSøkerBarn(behovmelding.ident, søknadId)

        // annen-pengestotte
        val harAndreYtelser = harAndreYtelser(behovmelding.ident, søknadId)

        // din-situasjon
        val ønskerDagpengerFraDato =
            commonBehovsløser.ønskerDagpengerFraDato(
                ident = behovmelding.ident,
                søknadId = søknadId,
            )

        // reell-arbeidssoker
        val reellArbeidssøker =
            erReellArbeidssøker(
                behovmelding.ident,
                søknadId,
            )

        val søknadsdataSTSBResultat =
            SøknadsdataSTSBResultat(
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

        val stsbResultJson = objectMapper.writeValueAsString(søknadsdataSTSBResultat)
        return publiserLøsning(behovmelding, stsbResultJson)
        //      return publiserLøsning(behovmelding, søknadId)
    }

    data class SøknadsdataSTSBResultat(
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
        val sluttårsak: Sluttårsaken,
        val fiskeforedling: Boolean,
        val land: String,
    ) {
        enum class Sluttårsaken {
            AVSKJEDIGET,
            ARBEIDSGIVER_KONKURS,
            KONTRAKT_UTGAATT,
            PERMITTERT,
            REDUSERT_ARBEIDSTID,
            SAGT_OPP_AV_ARBEIDSGIVER,
            SAGT_OPP_SELV,
            IKKE_ENDRET,
        }
    }

    data class ReellArbeidssøker(
        val helse: Boolean,
        val geografi: Boolean,
        val deltid: Boolean,
        val yrke: Boolean,
    )

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
    ): Boolean {
        val seksjonsSvar =
            seksjonRepository.hentSeksjonsvarEllerKastException(
                ident,
                søknadId,
                "verneplikt",
            )

        val seksjonsData = objectMapper.readTree(seksjonsSvar)
        return seksjonsData.finnOpplysning("avtjentVerneplikt").asText() == "ja"
    }

    fun finnAvsluttedeArbeidsforhold(
        ident: String,
        søknadId: UUID,
    ): List<AvsluttedeArbeidsforhold> {
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

    private fun JsonNode.sluttårsak(): Sluttårsaken =
        this.findPath("hvordanHarDetteArbeidsforholdetEndretSeg").asText().let {
            when (it) {
                "arbeidsforholdetErIkkeEndret" -> Sluttårsaken.IKKE_ENDRET
                "jegHarFåttAvskjed" -> Sluttårsaken.AVSKJEDIGET
                "arbeidsgiverenMinHarSagtMegOpp" -> Sluttårsaken.SAGT_OPP_AV_ARBEIDSGIVER
                "arbeidsgiverErKonkurs" -> Sluttårsaken.ARBEIDSGIVER_KONKURS
                "kontraktenErUtgått" -> Sluttårsaken.KONTRAKT_UTGAATT
                "jegHarSagtOppSelv" -> Sluttårsaken.SAGT_OPP_SELV
                "arbeidstidenErRedusert" -> Sluttårsaken.REDUSERT_ARBEIDSTID
                "jegErPermitert" -> Sluttårsaken.PERMITTERT
                else -> throw IllegalArgumentException("Ukjent sluttårsak: $it")
            }
        }

    private fun JsonNode.fiskForedling(): Boolean =
        this.finnOpplysning("permittertErDuPermittertFraFiskeforedlingsEllerFiskeoljeindustrien").asText() == "ja"

    fun eøsBostedsland(
        ident: String,
        søknadId: UUID,
    ): Boolean {
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
