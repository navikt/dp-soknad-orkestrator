package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.SøknadsdataSTSB
import no.nav.dagpenger.soknad.orkestrator.behov.FellesBehovløserLøsninger
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonRepository
import no.nav.dagpenger.soknad.orkestrator.utils.InMemoryQuizOpplysningRepository
import org.junit.jupiter.api.BeforeEach
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test

class SøknadsdataSTSBBehovsløserTest {
    val opplysningRepository = InMemoryQuizOpplysningRepository()
    val testRapid = TestRapid()
    val søknadRepository = mockk<SøknadRepository>(relaxed = true)
    val seksjonRepository = mockk<SeksjonRepository>(relaxed = true)
    val fellesBehovløserLøsninger: FellesBehovløserLøsninger =
        FellesBehovløserLøsninger(
            opplysningRepository,
            søknadRepository,
            seksjonRepository,
        )
    val behovløser =
        SøknadsdataSTSBBehovsløser(
            testRapid,
            opplysningRepository,
            søknadRepository,
            seksjonRepository,
            fellesBehovløserLøsninger,
        )
    val ident = "12345678910"
    val søknadId = UUID.randomUUID()
    val now = LocalDate.now()

    @BeforeEach
    fun setup() {
        every {
            søknadRepository.hentSøknadIdFraJournalPostId(any(), any())
        } returns søknadId

        every {
            seksjonRepository.hentSeksjonsvarEllerKastException(
                ident,
                søknadId,
                "din-situasjon",
            )
        } returns dinSituasjonMedGjenopptakelseOrkestratorJson(now)
    }

    // use grouped tests for eøsBostedsland true/false

    @Test
    fun `SøknadsdataSTSBBehovsløserTest eøsBostedsland cases`() {
        val cases =
            listOf(
                Triple("ja", "NOR", false),
                Triple("nei", "POL", true),
                Triple("nei", "BRA", false),
            )
        cases.forEach { (borINorge, bostedsland, expectedEøsBostedsland) ->
            every {
                seksjonRepository.hentSeksjonsvar(
                    søknadId,
                    ident,
                    "personalia",
                )
            } returns personaliaOrkestratorJson(borINorge, bostedsland)

            behovløser.løs(lagBehovmelding(ident, søknadId, SøknadsdataSTSB))

            testRapid.inspektør.message(0)["@løsning"]["SøknadsdataSTSB"].also { løsning ->
                løsning["verdi"].asText() shouldContain "\"eøsBostedsland\":$expectedEøsBostedsland"
                løsning["verdi"].asText() shouldContain "\"søknadId\":\"$søknadId\""
                løsning["verdi"].asText() shouldContain "\"ønskerDagpengerFraDato\":\"$now\""
            }
            testRapid.reset()
        }
    }

    @Test
    fun `SøknadsdataSTSBBehovsløserTest eøsArbeidsforhold cases`() {
        val cases =
            listOf(
                Pair("ja", true),
                Pair("nei", false),
            )
        cases.forEach { (eøsArbeidsforhold, expectedEøsArbeidsforhold) ->
            every {
                seksjonRepository.hentSeksjonsvarEllerKastException(
                    ident,
                    søknadId,
                    "arbeidsforhold",
                )
            } returns eøsArbeidsforholdOrkestratorJson(eøsArbeidsforhold)

            behovløser.løs(lagBehovmelding(ident, søknadId, SøknadsdataSTSB))

            testRapid.inspektør.message(0)["@løsning"]["SøknadsdataSTSB"].also { løsning ->
                løsning["verdi"].asText() shouldContain "\"eøsArbeidsforhold\":$expectedEøsArbeidsforhold"
                løsning["verdi"].asText() shouldContain "\"søknadId\":\"$søknadId\""
                løsning["verdi"].asText() shouldContain "\"ønskerDagpengerFraDato\":\"$now\""
            }
            testRapid.reset()
        }
    }

    @Test
    fun `SøknadsdataSTSBBehovsløserTest avtjentVerneplikt cases`() {
        val cases =
            listOf(
                Pair("ja", true),
                Pair("nei", false),
            )
        cases.forEach { (avtjentVerneplikt, expectedAvtjentVerneplikt) ->
            every {
                seksjonRepository.hentSeksjonsvarEllerKastException(
                    ident,
                    søknadId,
                    "verneplikt",
                )
            } returns avtjentVernepliktOrkestratorJson(avtjentVerneplikt)

            behovløser.løs(lagBehovmelding(ident, søknadId, SøknadsdataSTSB))
            testRapid.inspektør.message(0)["@løsning"]["SøknadsdataSTSB"].also { løsning ->
                løsning["verdi"].asText() shouldContain "\"avtjentVerneplikt\":$expectedAvtjentVerneplikt"
                løsning["verdi"].asText() shouldContain "\"søknadId\":\"$søknadId\""
                løsning["verdi"].asText() shouldContain "\"ønskerDagpengerFraDato\":\"$now\""
            }
            testRapid.reset()
        }
    }

    @Test
    fun `SøknadsdataSTSBBehovsløserTest har andre ytelser til true så lenge søkeren mottar minst en form for ytelse`() {
        val cases =
            listOf(
                Triple("ja", "nei", "nei"),
                Triple("nei", "ja", "nei"),
                Triple("nei", "nei", "ja"),
                Triple("ja", "ja", "ja"),
            )
        cases.forEach {
            (
                mottarDuEllerHarDuSøktOmPengestøtteFraAndreEnnNav,
                harMottattEllerSøktOmPengestøtteFraAndreEøsLand,
                fårEllerKommerTilÅFåLønnEllerAndreGoderFraTidligereArbeidsgiver,
            ),
            ->
            every {
                seksjonRepository.hentSeksjonsvar(
                    søknadId,
                    ident,
                    "annen-pengestotte",
                )
            } returns
                annenPengestøtteOrkestratorJson(
                    mottarDuEllerHarDuSøktOmPengestøtteFraAndreEnnNav,
                    harMottattEllerSøktOmPengestøtteFraAndreEøsLand,
                    fårEllerKommerTilÅFåLønnEllerAndreGoderFraTidligereArbeidsgiver,
                )

            behovløser.løs(lagBehovmelding(ident, søknadId, SøknadsdataSTSB))
            testRapid.inspektør.message(0)["@løsning"]["SøknadsdataSTSB"].also { løsning ->
                løsning["verdi"].asText() shouldContain "\"harAndreYtelser\":true"
                løsning["verdi"].asText() shouldContain "\"søknadId\":\"$søknadId\""
                løsning["verdi"].asText() shouldContain "\"ønskerDagpengerFraDato\":\"$now\""
            }
            testRapid.reset()
        }
    }

    @Test
    fun `SøknadsdataSTSBBehovsløserTest har andre ytelser til false så lenge søkeren mottar ingen form for ytelse`() {
        every {
            seksjonRepository.hentSeksjonsvar(
                søknadId,
                ident,
                "annen-pengestotte",
            )
        } returns
            annenPengestøtteOrkestratorJson(
                "nei",
                "nei",
                "nei",
            )

        behovløser.løs(lagBehovmelding(ident, søknadId, SøknadsdataSTSB))
        testRapid.inspektør.message(0)["@løsning"]["SøknadsdataSTSB"].also { løsning ->
            løsning["verdi"].asText() shouldContain "\"harAndreYtelser\":false"
            løsning["verdi"].asText() shouldContain "\"søknadId\":\"$søknadId\""
            løsning["verdi"].asText() shouldContain "\"ønskerDagpengerFraDato\":\"$now\""
        }
    }

    @Test
    fun `Avsluttet arbeidsforhold grunnet permittert fra fiskeforedling eller fiskeindustrien`() {
        every {
            seksjonRepository.hentSeksjonsvarEllerKastException(
                ident,
                søknadId,
                "arbeidsforhold",
            )
        } returns arbeidsforholdMedRegistrerteAvsluttedeArbeidsforholdOrkestratorJson()

        behovløser.løs(lagBehovmelding(ident, søknadId, SøknadsdataSTSB))
        testRapid.inspektør.message(0)["@løsning"]["SøknadsdataSTSB"].also { løsning ->
            løsning["verdi"].asText() shouldContain
                "{\"sluttårsak\":\"SAGT_OPP_AV_ARBEIDSGIVER\",\"fiskeforedling\":false,\"land\":\"NOR\"},{\"sluttårsak\":\"PERMITTERT\",\"fiskeforedling\":true,\"land\":\"SWE\"}"
            løsning["verdi"].asText() shouldContain "\"søknadId\":\"$søknadId\""
            løsning["verdi"].asText() shouldContain "\"ønskerDagpengerFraDato\":\"$now\""
        }
    }

    @Test
    fun `Søker med arbeidsforhold returneres med liste over sluttårsak, permittert fra fiskeforedling og land de jobbet i`() {
        every {
            seksjonRepository.hentSeksjonsvarEllerKastException(
                ident,
                søknadId,
                "arbeidsforhold",
            )
        } returns arbeidsforholdMedRegistrerteAvsluttedeArbeidsforholdOrkestratorJson()

        behovløser.løs(lagBehovmelding(ident, søknadId, SøknadsdataSTSB))
        testRapid.inspektør.message(0)["@løsning"]["SøknadsdataSTSB"].also { løsning ->
            løsning["verdi"].asText() shouldContain
                "{\"sluttårsak\":\"SAGT_OPP_AV_ARBEIDSGIVER\",\"fiskeforedling\":false,\"land\":\"NOR\"},{\"sluttårsak\":\"PERMITTERT\",\"fiskeforedling\":true,\"land\":\"SWE\"}"
            løsning["verdi"].asText() shouldContain "\"søknadId\":\"$søknadId\""
            løsning["verdi"].asText() shouldContain "\"ønskerDagpengerFraDato\":\"$now\""
        }
    }

    @Test
    fun `Søker uten arbeidsforhold returneres som tom liste`() {
        every {
            seksjonRepository.hentSeksjonsvarEllerKastException(
                ident,
                søknadId,
                "arbeidsforhold",
            )
        } returns arbeidsforholdUtenRegistrerteAvsluttedeArbeidsforholdOrkestratorJson()

        behovløser.løs(lagBehovmelding(ident, søknadId, SøknadsdataSTSB))
        testRapid.inspektør.message(0)["@løsning"]["SøknadsdataSTSB"].also { løsning ->
            løsning["verdi"].asText() shouldContain
                "\"avsluttetArbeidsforhold\":[]"
            løsning["verdi"].asText() shouldContain "\"søknadId\":\"$søknadId\""
            løsning["verdi"].asText() shouldContain "\"ønskerDagpengerFraDato\":\"$now\""
        }
    }

    @Test
    fun `Søker med kun barn fra PDL får harBarn verdien satt til true`() {
        every {
            seksjonRepository.hentSeksjonsvarEllerKastException(
                ident,
                søknadId,
                "barnetillegg",
            )
        } returns barnetilleggMedToBarnFraPdlOgUtenManuelLagteBarn()

        behovløser.løs(lagBehovmelding(ident, søknadId, SøknadsdataSTSB))
        testRapid.inspektør.message(0)["@løsning"]["SøknadsdataSTSB"].also { løsning ->
            løsning["verdi"].asText() shouldContain
                "\"harBarn\":true"
            løsning["verdi"].asText() shouldContain "\"søknadId\":\"$søknadId\""
            løsning["verdi"].asText() shouldContain "\"ønskerDagpengerFraDato\":\"$now\""
        }
    }

    @Test
    fun `Søker med barn fra PDL og manuel lagte barn får harBarn verdien satt til true`() {
        every {
            seksjonRepository.hentSeksjonsvarEllerKastException(
                ident,
                søknadId,
                "barnetillegg",
            )
        } returns barnetilleggMedBarnFraPdlOgManueltLagteBarn()

        behovløser.løs(lagBehovmelding(ident, søknadId, SøknadsdataSTSB))
        testRapid.inspektør.message(0)["@løsning"]["SøknadsdataSTSB"].also { løsning ->
            løsning["verdi"].asText() shouldContain
                "\"harBarn\":true"
            løsning["verdi"].asText() shouldContain "\"søknadId\":\"$søknadId\""
            løsning["verdi"].asText() shouldContain "\"ønskerDagpengerFraDato\":\"$now\""
        }
    }

    @Test
    fun `Søker med kun barn lagt manuel uten barn fra PDL får harBarn verdien satt til true`() {
        every {
            seksjonRepository.hentSeksjonsvarEllerKastException(
                ident,
                søknadId,
                "barnetillegg",
            )
        } returns barnetilleggMedBarnLagtManuelUtenBarnFraPdl()

        behovløser.løs(lagBehovmelding(ident, søknadId, SøknadsdataSTSB))
        testRapid.inspektør.message(0)["@løsning"]["SøknadsdataSTSB"].also { løsning ->
            løsning["verdi"].asText() shouldContain
                "\"harBarn\":true"
            løsning["verdi"].asText() shouldContain "\"søknadId\":\"$søknadId\""
            løsning["verdi"].asText() shouldContain "\"ønskerDagpengerFraDato\":\"$now\""
        }
    }

    @Test
    fun `Søker med ingen barn får harBarn verdien satt til false`() {
        every {
            seksjonRepository.hentSeksjonsvarEllerKastException(
                ident,
                søknadId,
                "barnetillegg",
            )
        } returns barnetilleggUtenBarn()

        behovløser.løs(lagBehovmelding(ident, søknadId, SøknadsdataSTSB))
        testRapid.inspektør.message(0)["@løsning"]["SøknadsdataSTSB"].also { løsning ->
            løsning["verdi"].asText() shouldContain
                "\"harBarn\":false"
            løsning["verdi"].asText() shouldContain "\"søknadId\":\"$søknadId\""
            løsning["verdi"].asText() shouldContain "\"ønskerDagpengerFraDato\":\"$now\""
        }
    }

    @Test
    fun `Teste reell-arbeidssøker hvor noen verdier er true`() {
        every {
            seksjonRepository.hentSeksjonsvarEllerKastException(
                ident,
                søknadId,
                "reell-arbeidssoker",
            )
        } returns
            reellArbeidssøkerOrkestratorJson(
                kanDuTaAlleTyperArbeid = "ja",
                kanDuJobbeIHeleNorge = "nei",
                kanDuJobbeBådeHeltidOgDeltid = "ja",
                erDuVilligTilÅBytteYrkeEllerGåNedILønn = "nei",
            )

        behovløser.løs(lagBehovmelding(ident, søknadId, SøknadsdataSTSB))
        testRapid.inspektør.message(0)["@løsning"]["SøknadsdataSTSB"].also { løsning ->
            løsning["verdi"].asText() shouldContain
                "\"helse\":true,\"geografi\":false,\"deltid\":true,\"yrke\":false"
            løsning["verdi"].asText() shouldContain "\"søknadId\":\"$søknadId\""
            løsning["verdi"].asText() shouldContain "\"ønskerDagpengerFraDato\":\"$now\""
        }
    }

    @Test
    fun `Teste reell-arbeidssøker hvor alle verdier er true`() {
        every {
            seksjonRepository.hentSeksjonsvarEllerKastException(
                ident,
                søknadId,
                "reell-arbeidssoker",
            )
        } returns
            reellArbeidssøkerOrkestratorJson(
                kanDuTaAlleTyperArbeid = "ja",
                kanDuJobbeIHeleNorge = "ja",
                kanDuJobbeBådeHeltidOgDeltid = "ja",
                erDuVilligTilÅBytteYrkeEllerGåNedILønn = "ja",
            )

        behovløser.løs(lagBehovmelding(ident, søknadId, SøknadsdataSTSB))
        testRapid.inspektør.message(0)["@løsning"]["SøknadsdataSTSB"].also { løsning ->
            løsning["verdi"].asText() shouldContain
                "\"helse\":true,\"geografi\":true,\"deltid\":true,\"yrke\":true"
            løsning["verdi"].asText() shouldContain "\"søknadId\":\"$søknadId\""
            løsning["verdi"].asText() shouldContain "\"ønskerDagpengerFraDato\":\"$now\""
        }
    }

    @Test
    fun `Teste reell-arbeidssøker hvor ingen verdier er true`() {
        every {
            seksjonRepository.hentSeksjonsvarEllerKastException(
                ident,
                søknadId,
                "reell-arbeidssoker",
            )
        } returns
            reellArbeidssøkerOrkestratorJson(
                kanDuTaAlleTyperArbeid = "nei",
                kanDuJobbeIHeleNorge = "nei",
                kanDuJobbeBådeHeltidOgDeltid = "nei",
                erDuVilligTilÅBytteYrkeEllerGåNedILønn = "nei",
            )

        behovløser.løs(lagBehovmelding(ident, søknadId, SøknadsdataSTSB))
        testRapid.inspektør.message(0)["@løsning"]["SøknadsdataSTSB"].also { løsning ->
            løsning["verdi"].asText() shouldContain
                "\"helse\":false,\"geografi\":false,\"deltid\":false,\"yrke\":false"
            løsning["verdi"].asText() shouldContain "\"søknadId\":\"$søknadId\""
            løsning["verdi"].asText() shouldContain "\"ønskerDagpengerFraDato\":\"$now\""
        }
    }
}

fun personaliaOrkestratorJson(
    borINorge: String,
    bostedsland: String,
): String =
    "{\"fornavnFraPdl\":\"TOPP\",\"mellomnavnFraPdl\":\"\",\"etternavnFraPdl\":\"SURE\",\"fødselsnummerFraPdl\":\"21857998666\",\"alderFraPdl\":\"46\",\"adresselinje1FraPdl\":\"Dale 17\",\"adresselinje2FraPdl\":\"\",\"adresselinje3FraPdl\":\"\",\"postnummerFraPdl\":\"9423\",\"poststedFraPdl\":\"Grøtavær\",\"landkodeFraPdl\":\"NO\",\"landFraPdl\":\"NORGE\",\"kontonummerFraKontoregister\":\"\",\"folkeregistrertAdresseErNorgeStemmerDet\":\"$borINorge\",\"bostedsland\":\"$bostedsland\"}"
        .trimIndent()

fun eøsArbeidsforholdOrkestratorJson(eøsArbeidsforhold: String): String =
    "{\"seksjonId\":\"arbeidsforhold\",\"seksjonsvar\":{\"hvordanHarDuJobbet\":\"fastArbeidstidIMindreEnn6Måneder\",\"harDuJobbetIEtAnnetEøsLandSveitsEllerStorbritanniaILøpetAvDeSiste36Månedene\":\"$eøsArbeidsforhold\",\"registrerteArbeidsforhold\":[{\"navnetPåBedriften\":\"asdasd\",\"hvilketLandJobbetDuI\":\"NOR\",\"varighetPåArbeidsforholdetFraOgMedDato\":\"2024-01-01\",\"varighetPåArbeidsforholdetTilOgMedDato\":\"2025-11-27\",\"hvordanHarDetteArbeidsforholdetEndretSeg\":\"arbeidsgiverenMinHarSagtMegOpp\",\"jegErOppsagtHvaVarÅrsaken\":\"sdfsfd\",\"jegErOppsagtHarDuFåttTilbudOmÅFortsetteHosArbeidsgiverenDinIAnnenStillingEllerEtAnnetStedINorge\":\"nei\",\"harDuJobbetSkiftTurnusEllerRotasjon\":\"hverken-skift-turnus-eller-rotasjon\",\"id\":\"f047539f-6911-4902-9af5-f1b85545496c\",\"dokumentasjonskrav\":[\"533bdc6d-a3ba-4936-ace2-cd455aaf86ab\",\"a0ec261c-631e-4d2e-8c92-ecabcd399eab\"]}]},\"versjon\":1}"
        .trimIndent()

fun avtjentVernepliktOrkestratorJson(avtjentVerneplikt: String): String =
    "{\"avtjentVerneplikt\":\"$avtjentVerneplikt\",\"dokumentasjonskrav\":\"null\"}".trimIndent()

fun annenPengestøtteOrkestratorJson(
    mottarDuEllerHarDuSøktOmPengestøtteFraAndreEnnNav: String,
    harMottattEllerSøktOmPengestøtteFraAndreEøsLand: String,
    fårEllerKommerTilÅFåLønnEllerAndreGoderFraTidligereArbeidsgiver: String,
): String =
    (
        "{\"mottarDuEllerHarDuSøktOmPengestøtteFraAndreEnnNav\":\"$mottarDuEllerHarDuSøktOmPengestøtteFraAndreEnnNav\", " +
            "\"harMottattEllerSøktOmPengestøtteFraAndreEøsLand\":\"$harMottattEllerSøktOmPengestøtteFraAndreEøsLand\", " +
            "\"fårEllerKommerTilÅFåLønnEllerAndreGoderFraTidligereArbeidsgiver\":\"$fårEllerKommerTilÅFåLønnEllerAndreGoderFraTidligereArbeidsgiver\"}"
    ).trimIndent()

fun arbeidsforholdMedRegistrerteAvsluttedeArbeidsforholdOrkestratorJson(): String =
    "{\"hvordanHarDuJobbet\":\"jobbetMerIGjennomsnittDeSiste36MånedeneEnnDeSiste12Månedene\",\"harDuJobbetIEtAnnetEøsLandSveitsEllerStorbritanniaILøpetAvDeSiste36Månedene\":\"nei\",\"registrerteArbeidsforhold\":[{\"navnetPåBedriften\":\"Oslo burger og strøm\",\"hvilketLandJobbetDuI\":\"NOR\",\"varighetPåArbeidsforholdetFraOgMedDato\":\"2025-11-19\",\"varighetPåArbeidsforholdetTilOgMedDato\":\"2025-11-28\",\"hvordanHarDetteArbeidsforholdetEndretSeg\":\"arbeidsgiverenMinHarSagtMegOpp\",\"jegErOppsagtHvaVarÅrsaken\":\"asd\",\"jegErOppsagtHarDuFåttTilbudOmÅFortsetteHosArbeidsgiverenDinIAnnenStillingEllerEtAnnetStedINorge\":\"nei\",\"harDuJobbetSkiftTurnusEllerRotasjon\":\"hverken-skift-turnus-eller-rotasjon\",\"id\":\"1c01dec1-0738-4ed9-a6fb-fb75a0730302\",\"dokumentasjonskrav\":[\"6bf2a891-5be3-4477-8955-4c0c488735db\",\"c23b0fa4-e4ad-4734-b780-66746c4bd3b2\"]},{\"navnetPåBedriften\":\"LAVE HESTER AS\",\"hvilketLandJobbetDuI\":\"SWE\",\"oppgiPersonnummeretPinDuHaddeIDetteLandet\":\"12431441\",\"varighetPåArbeidsforholdetFraOgMedDato\":\"2025-11-05\",\"hvordanHarDetteArbeidsforholdetEndretSeg\":\"jegErPermitert\",\"permittertErDetteEtMidlertidigArbeidsforholdMedEnKontraktfestetSluttdato\":\"ja\",\"permittertOppgiDenKontraktsfestedeSluttdatoenPåDetteArbeidsforholdet\":\"2025-11-03\",\"permittertNårStartetDuIDenneJobben\":\"2025-11-27\",\"permittertErDuPermittertFraFiskeforedlingsEllerFiskeoljeindustrien\":\"ja\",\"permittertNårErDuPermittertFraOgMedDato\":\"2025-11-03\",\"permittertNårErDuPermittertTilOgMedDato\":\"2025-11-29\",\"permittertHvorMangeProsentErDuPermittert\":\"12\",\"permittertVetDuNårLønnspliktperiodenTilArbeidsgiverenDinEr\":\"ja\",\"permittertLønnsperiodeFraOgMedDato\":\"2025-11-24\",\"permittertLønnsperiodeTilOgMedDato\":\"2025-11-30\",\"harDuJobbetSkiftTurnusEllerRotasjon\":\"rotasjon\",\"hvilkenTypeRotasjonsordningJobbetDu\":\"2-3-rotasjon\",\"oppgiSisteArbeidsperiodeIDenSisteRotasjonenDinDatoFraOgMed\":\"2025-11-12\",\"oppgiSisteArbeidsperiodeIDenSisteRotasjonenDinDatoTilOgMed\":\"2025-11-28\"}]}"
        .trimIndent()

fun arbeidsforholdUtenRegistrerteAvsluttedeArbeidsforholdOrkestratorJson(): String =
    "{\"hvordan-har-du-jobbet\":\"har-ikke-jobbet-de-siste-36-månedene\",\"registrerteArbeidsforhold\":[]}".trimIndent()

fun dinSituasjonMedGjenopptakelseOrkestratorJson(now: LocalDate): String =
    "{\"harDuMottattDagpengerFraNavILøpetAvDeSiste52Ukene\": \"ja\",\"årsakTilAtDagpengeneBleStanset\": \"dfg\",\"hvilkenDatoSøkerDuGjenopptakFra\": \"$now\"}"

fun barnetilleggMedToBarnFraPdlOgUtenManuelLagteBarn(): String =
    "{\"seksjonId\":\"barnetillegg\",\"versjon\":1,\"seksjonsvar\":{\"barnFraPdl\":[{\"id\":\"e27be3cc-1392-4e38-bf48-4200809da68b\",\"fornavn\":\"SMISKENDE\",\"mellomnavn\":\"\",\"fornavnOgMellomnavn\":\"SMISKENDE\",\"etternavn\":\"KJENNING\",\"fødselsdato\":\"2013-05-26\",\"alder\":12,\"bostedsland\":\"NOR\",\"forsørgerDuBarnet\":\"nei\"},{\"id\":\"0113251a-420e-44d8-99d2-9ba7c5e89aac\",\"fornavn\":\"ENGASJERT\",\"mellomnavn\":\"\",\"fornavnOgMellomnavn\":\"ENGASJERT\",\"etternavn\":\"BUSSTOPP\",\"fødselsdato\":\"2009-11-12\",\"alder\":16,\"bostedsland\":\"NOR\",\"forsørgerDuBarnet\":\"nei\"}],\"forsørgerDuBarnSomIkkeVisesHer\":\"nei\",\"barnLagtManuelt\":null}}"

fun barnetilleggMedBarnLagtManuelUtenBarnFraPdl(): String =
    "{\"seksjonId\":\"barnetillegg\",\"versjon\":1,\"seksjonsvar\":{\"barnFraPdl\":null,\"forsørgerDuBarnSomIkkeVisesHer\":\"nei\",\"barnLagtManuelt\":[{\"id\":\"0113251a-420e-44d8-99d2-9ba7c5e89aac\",\"fornavn\":\"ENGASJERT\",\"mellomnavn\":\"\",\"fornavnOgMellomnavn\":\"ENGASJERT\",\"etternavn\":\"BUSSTOPP\",\"fødselsdato\":\"2009-11-12\",\"alder\":16,\"bostedsland\":\"NOR\",\"forsørgerDuBarnet\":\"nei\"}]}}"

fun barnetilleggUtenBarn(): String =
    "{\"seksjonId\":\"barnetillegg\",\"versjon\":1,\"seksjonsvar\":{\"barnFraPdl\":null,\"forsørgerDuBarnSomIkkeVisesHer\":\"nei\",\"barnLagtManuelt\":null}}"

fun barnetilleggMedBarnFraPdlOgManueltLagteBarn(): String =
    "{\"seksjonId\":\"barnetillegg\",\"versjon\":1,\"seksjonsvar\":{\"barnFraPdl\":[{\"id\":\"0113251a-420e-44d8-99d2-9ba7c5e89aac\",\"fornavn\":\"ENGASJERT\",\"mellomnavn\":\"\",\"fornavnOgMellomnavn\":\"ENGASJERT\",\"etternavn\":\"BUSSTOPP\",\"fødselsdato\":\"2009-11-12\",\"alder\":16,\"bostedsland\":\"NOR\",\"forsørgerDuBarnet\":\"nei\"}],\"forsørgerDuBarnSomIkkeVisesHer\":\"nei\",\"barnLagtManuelt\":[{\"id\":\"e27be3cc-1392-4e38-bf48-4200809da68b\",\"fornavn\":\"SMISKENDE\",\"mellomnavn\":\"\",\"fornavnOgMellomnavn\":\"SMISKENDE\",\"etternavn\":\"KJENNING\",\"fødselsdato\":\"2013-05-26\",\"alder\":12,\"bostedsland\":\"NOR\",\"forsørgerDuBarnet\":\"nei\"}]}}"

fun reellArbeidssøkerOrkestratorJson(
    kanDuTaAlleTyperArbeid: String,
    kanDuJobbeIHeleNorge: String,
    kanDuJobbeBådeHeltidOgDeltid: String,
    erDuVilligTilÅBytteYrkeEllerGåNedILønn: String,
): String =
    "{\"seksjonId\":\"reell-arbeidssoker\",\"seksjonsvar\":{\"kanDuJobbeBådeHeltidOgDeltid\":\"$kanDuJobbeBådeHeltidOgDeltid\",\"kanDuJobbeIHeleNorge\":\"$kanDuJobbeIHeleNorge\",\"kanDuTaAlleTyperArbeid\":\"$kanDuTaAlleTyperArbeid\",\"erDuVilligTilÅBytteYrkeEllerGåNedILønn\":\"$erDuVilligTilÅBytteYrkeEllerGåNedILønn\",\"dokumentasjonskrav\":\"null\"},\"versjon\":1}"
