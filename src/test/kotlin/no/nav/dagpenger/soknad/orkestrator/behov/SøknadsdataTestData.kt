package no.nav.dagpenger.soknad.orkestrator.behov

import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.QuizOpplysning
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.BarnSvar
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Boolsk
import java.time.LocalDate
import java.util.UUID

fun personaliaOrkestratorJson(
    borINorge: String,
    bostedsland: String,
): String =
    //language=JSON
    """
{
  "seksjonId": "personalia",
  "seksjonsvar": {
    "fornavnFraPdl": "TOPP",
    "mellomnavnFraPdl": "",
    "etternavnFraPdl": "SURE",
    "fødselsnummerFraPdl": "21857998666",
    "alderFraPdl": "46",
    "adresselinje1FraPdl": "Dale 17",
    "adresselinje2FraPdl": "",
    "adresselinje3FraPdl": "",
    "postnummerFraPdl": "9423",
    "poststedFraPdl": "Grøtavær",
    "landkodeFraPdl": "NO",
    "landFraPdl": "NORGE",
    "kontonummerFraKontoregister": "",
    "folkeregistrertAdresseErNorgeStemmerDet": "$borINorge",
    "bostedsland": "$bostedsland"
  },
  "versjon": 1
}
    """.trimIndent()

fun eøsArbeidsforholdOrkestratorJson(eøsArbeidsforhold: String): String =
    //language=JSON
    """
{
  "seksjonId": "arbeidsforhold",
  "seksjonsvar": {
    "hvordanHarDuJobbet": "fastArbeidstidIMindreEnn6Måneder",
    "harDuJobbetIEtAnnetEøsLandSveitsEllerStorbritanniaILøpetAvDeSiste36Månedene": "$eøsArbeidsforhold",
    "registrerteArbeidsforhold": [{
      "navnetPåBedriften": "asdasd",
      "hvilketLandJobbetDuI": "NOR",
      "varighetPåArbeidsforholdetFraOgMedDato": "2024-01-01",
      "varighetPåArbeidsforholdetTilOgMedDato": "2025-11-27",
      "hvordanHarDetteArbeidsforholdetEndretSeg": "arbeidsgiverenMinHarSagtMegOpp",
      "jegErOppsagtHvaVarÅrsaken": "sdfsfd",
      "jegErOppsagtHarDuFåttTilbudOmÅFortsetteHosArbeidsgiverenDinIAnnenStillingEllerEtAnnetStedINorge": "nei",
      "harDuJobbetSkiftTurnusEllerRotasjon": "hverken-skift-turnus-eller-rotasjon",
      "id": "f047539f-6911-4902-9af5-f1b85545496c",
      "dokumentasjonskrav": ["533bdc6d-a3ba-4936-ace2-cd455aaf86ab", "a0ec261c-631e-4d2e-8c92-ecabcd399eab"]
    }]
  },
  "versjon": 1
}
    """.trimIndent()

fun avtjentVernepliktOrkestratorJson(avtjentVerneplikt: String): String =
    //language=JSON
    """
{
  "seksjonId": "verneplikt",
  "seksjonsvar": {
    "avtjentVerneplikt": "$avtjentVerneplikt",
    "dokumentasjonskrav": null
  },
  "versjon": 1
}
    """.trimIndent()

fun annenPengestøtteOrkestratorJson(
    mottarDuPengestøtteFraAndreEnnNav: String,
    mottarDuAndreUtbetalingerEllerØkonomiskeGoderFraTidligereArbeidsgiver: String,
): String =
    //language=JSON
    """
{
  "seksjonId": "annen-pengestotte",
  "seksjonsvar": {
    "mottarDuPengestøtteFraAndreEnnNav": "$mottarDuPengestøtteFraAndreEnnNav",
    "mottarDuAndreUtbetalingerEllerØkonomiskeGoderFraTidligereArbeidsgiver": "$mottarDuAndreUtbetalingerEllerØkonomiskeGoderFraTidligereArbeidsgiver"
  },
  "versjon": 1
}
    """.trimIndent()

fun arbeidsforholdMedRegistrerteAvsluttedeArbeidsforholdOrkestratorJson(): String =
    //language=JSON
    """
{
  "seksjonId": "arbeidsforhold",
  "seksjonsvar": {
    "hvordanHarDuJobbet": "jobbetMerIGjennomsnittDeSiste36MånedeneEnnDeSiste12Månedene",
    "harDuJobbetIEtAnnetEøsLandSveitsEllerStorbritanniaILøpetAvDeSiste36Månedene": "nei",
    "registrerteArbeidsforhold": [{
      "navnetPåBedriften": "Oslo burger og strøm",
      "hvilketLandJobbetDuI": "NOR",
      "varighetPåArbeidsforholdetFraOgMedDato": "2025-11-19",
      "varighetPåArbeidsforholdetTilOgMedDato": "2025-11-28",
      "hvordanHarDetteArbeidsforholdetEndretSeg": "arbeidsgiverenMinHarSagtMegOpp",
      "jegErOppsagtHvaVarÅrsaken": "asd",
      "jegErOppsagtHarDuFåttTilbudOmÅFortsetteHosArbeidsgiverenDinIAnnenStillingEllerEtAnnetStedINorge": "nei",
      "harDuJobbetSkiftTurnusEllerRotasjon": "hverken-skift-turnus-eller-rotasjon",
      "id": "1c01dec1-0738-4ed9-a6fb-fb75a0730302",
      "dokumentasjonskrav": ["6bf2a891-5be3-4477-8955-4c0c488735db", "c23b0fa4-e4ad-4734-b780-66746c4bd3b2"]
    }, {
      "navnetPåBedriften": "LAVE HESTER AS",
      "hvilketLandJobbetDuI": "SWE",
      "oppgiPersonnummeretPinDuHaddeIDetteLandet": "12431441",
      "varighetPåArbeidsforholdetFraOgMedDato": "2025-11-05",
      "hvordanHarDetteArbeidsforholdetEndretSeg": "jegErPermitert",
      "permittertErDetteEtMidlertidigArbeidsforholdMedEnKontraktfestetSluttdato": "ja",
      "permittertOppgiDenKontraktsfestedeSluttdatoenPåDetteArbeidsforholdet": "2025-11-03",
      "permittertNårStartetDuIDenneJobben": "2025-11-27",
      "permittertErDuPermittertFraFiskeforedlingsEllerFiskeoljeindustrien": "ja",
      "permittertNårErDuPermittertFraOgMedDato": "2025-11-03",
      "permittertNårErDuPermittertTilOgMedDato": "2025-11-29",
      "permittertHvorMangeProsentErDuPermittert": "12",
      "permittertVetDuNårLønnspliktperiodenTilArbeidsgiverenDinEr": "ja",
      "permittertLønnsperiodeFraOgMedDato": "2025-11-24",
      "permittertLønnsperiodeTilOgMedDato": "2025-11-30",
      "harDuJobbetSkiftTurnusEllerRotasjon": "rotasjon",
      "hvilkenTypeRotasjonsordningJobbetDu": "2-3-rotasjon",
      "oppgiSisteArbeidsperiodeIDenSisteRotasjonenDinDatoFraOgMed": "2025-11-12",
      "oppgiSisteArbeidsperiodeIDenSisteRotasjonenDinDatoTilOgMed": "2025-11-28"
    }]
  },
  "versjon": 1
}
    """.trimIndent()

fun arbeidsforholdUtenRegistrerteAvsluttedeArbeidsforholdOrkestratorJson(): String =
    //language=JSON
    """
{
  "seksjonId": "arbeidsforhold",
  "seksjonsvar": {
    "hvordanHarDuJobbet": "har-ikke-jobbet-de-siste-36-månedene",
    "registrerteArbeidsforhold": []
  },
  "versjon": 1
}
    """.trimIndent()

fun dinSituasjonMedGjenopptakelseOrkestratorJson(now: LocalDate): String =
    //language=JSON
    """
{
  "seksjonId": "din-situasjon",
  "seksjonsvar": {
    "harDuMottattDagpengerFraNavILøpetAvDeSiste52Ukene": "ja",
    "årsakTilAtDagpengeneBleStanset": "dfg",
    "hvilkenDatoSøkerDuGjenopptakFra": "$now"
  },
  "versjon": 1
}
    """.trimIndent()

fun dinSituasjonMedDagpengerFraDato(now: LocalDate): String =
    //language=JSON
    """
{
  "seksjonId": "din-situasjon",
  "seksjonsvar": {
    "harDuMottattDagpengerFraNavILøpetAvDeSiste52Ukene": "nei",
    "hvilkenDatoSøkerDuDagpengerFra": "$now"
  },
  "versjon": 1
}
    """.trimIndent()

fun barnetilleggMedToBarnFraPdlOgUtenManuelLagteBarn(): String =
    //language=JSON
    """
{
  "seksjonId": "barnetillegg",
  "seksjonsvar": {
    "barnFraPdl": [{
      "id": "e27be3cc-1392-4e38-bf48-4200809da68b",
      "fornavn": "SMISKENDE",
      "mellomnavn": "",
      "fornavnOgMellomnavn": "SMISKENDE",
      "etternavn": "KJENNING",
      "fødselsdato": "2013-05-26",
      "alder": 12,
      "bostedsland": "NOR",
      "forsørgerDuBarnet": "nei"
    }, {
      "id": "0113251a-420e-44d8-99d2-9ba7c5e89aac",
      "fornavn": "ENGASJERT",
      "mellomnavn": "",
      "fornavnOgMellomnavn": "ENGASJERT",
      "etternavn": "BUSSTOPP",
      "fødselsdato": "2009-11-12",
      "alder": 16,
      "bostedsland": "NOR",
      "forsørgerDuBarnet": "nei"
    }],
    "forsørgerDuBarnSomIkkeVisesHer": "nei",
    "barnLagtManuelt": null
  },
  "versjon": 1
}
    """.trimIndent()

fun barnetilleggMedBarnLagtManuelUtenBarnFraPdl(): String =
    //language=JSON
    """
{
  "seksjonId": "barnetillegg",
  "seksjonsvar": {
    "barnFraPdl": null,
    "forsørgerDuBarnSomIkkeVisesHer": "nei",
    "barnLagtManuelt": [{
      "id": "0113251a-420e-44d8-99d2-9ba7c5e89aac",
      "fornavn": "ENGASJERT",
      "mellomnavn": "",
      "fornavnOgMellomnavn": "ENGASJERT",
      "etternavn": "BUSSTOPP",
      "fødselsdato": "2009-11-12",
      "alder": 16,
      "bostedsland": "NOR",
      "forsørgerDuBarnet": "nei"
    }]
  },
  "versjon": 1
}
    """.trimIndent()

fun barnetilleggUtenBarn(): String =
    //language=JSON
    """
{
  "seksjonId": "barnetillegg",
  "seksjonsvar": {
    "barnFraPdl": null,
    "forsørgerDuBarnSomIkkeVisesHer": "nei",
    "barnLagtManuelt": null
  },
  "versjon": 1
}
    """.trimIndent()

fun barnetilleggMedBarnFraPdlOgManueltLagteBarn(): String =
    //language=JSON
    """
{
  "seksjonId": "barnetillegg",
  "seksjonsvar": {
    "barnFraPdl": [{
      "id": "0113251a-420e-44d8-99d2-9ba7c5e89aac",
      "fornavn": "ENGASJERT",
      "mellomnavn": "",
      "fornavnOgMellomnavn": "ENGASJERT",
      "etternavn": "BUSSTOPP",
      "fødselsdato": "2009-11-12",
      "alder": 16,
      "bostedsland": "NOR",
      "forsørgerDuBarnet": "nei"
    }],
    "forsørgerDuBarnSomIkkeVisesHer": "nei",
    "barnLagtManuelt": [{
      "id": "e27be3cc-1392-4e38-bf48-4200809da68b",
      "fornavn": "SMISKENDE",
      "mellomnavn": "",
      "fornavnOgMellomnavn": "SMISKENDE",
      "etternavn": "KJENNING",
      "fødselsdato": "2013-05-26",
      "alder": 12,
      "bostedsland": "NOR",
      "forsørgerDuBarnet": "nei"
    }]
  },
  "versjon": 1
}
    """.trimIndent()

fun reellArbeidssøkerOrkestratorJson(
    kanDuTaAlleTyperArbeid: String,
    kanDuJobbeIHeleNorge: String,
    kanDuJobbeBådeHeltidOgDeltid: String,
    erDuVilligTilÅBytteYrkeEllerGåNedILønn: String,
): String =
    //language=JSON
    """
{
  "seksjonId": "reell-arbeidssoker",
  "seksjonsvar": {
    "kanDuJobbeBådeHeltidOgDeltid": "$kanDuJobbeBådeHeltidOgDeltid",
    "kanDuJobbeIHeleNorge": "$kanDuJobbeIHeleNorge",
    "kanDuTaAlleTyperArbeid": "$kanDuTaAlleTyperArbeid",
    "erDuVilligTilÅBytteYrkeEllerGåNedILønn": "$erDuVilligTilÅBytteYrkeEllerGåNedILønn",
    "dokumentasjonskrav": null
  },
  "versjon": 1
}
    """.trimIndent()

fun pdlBarnFraQuiz() =
    listOf(
        BarnSvar(
            barnSvarId = UUID.randomUUID(),
            fornavnOgMellomnavn = "Sure",
            etternavn = "Sopp",
            `fødselsdato` = LocalDate.now().minusYears(3),
            statsborgerskap = "NOR",
            `forsørgerBarnet` = true,
            fraRegister = true,
            kvalifisererTilBarnetillegg = true,
        ),
        BarnSvar(
            barnSvarId = UUID.randomUUID(),
            fornavnOgMellomnavn = "Lure",
            etternavn = "Laks",
            `fødselsdato` = LocalDate.now().minusYears(2),
            statsborgerskap = "NOR",
            `forsørgerBarnet` = true,
            fraRegister = true,
            kvalifisererTilBarnetillegg = true,
        ),
    )

fun manuelLagteBarnFraQuiz() =
    listOf(
        BarnSvar(
            barnSvarId = UUID.randomUUID(),
            fornavnOgMellomnavn = "Glade",
            etternavn = "Gås",
            `fødselsdato` = LocalDate.now().minusYears(1),
            statsborgerskap = "NOR",
            `forsørgerBarnet` = true,
            fraRegister = true,
            kvalifisererTilBarnetillegg = true,
        ),
        BarnSvar(
            barnSvarId = UUID.randomUUID(),
            fornavnOgMellomnavn = "Triste",
            etternavn = "Torsk",
            `fødselsdato` = LocalDate.now(),
            statsborgerskap = "NOR",
            `forsørgerBarnet` = true,
            fraRegister = true,
            kvalifisererTilBarnetillegg = true,
        ),
    )

fun hentAlleTypeArbeidForReellArbeidssøkerFraQuiz(
    ident: String,
    søknadId: UUID,
    svar: Boolean,
): QuizOpplysning<*> =
    QuizOpplysning(
        beskrivendeId = "faktum.alle-typer-arbeid",
        type = Boolsk,
        svar = svar,
        ident = ident,
        søknadId = søknadId,
    )

fun hentKanDuJobbeIHeleNorgeForReellArbeidssøkerFraQuiz(
    ident: String,
    søknadId: UUID,
    svar: Boolean,
) = QuizOpplysning(
    beskrivendeId = "faktum.jobbe-hele-norge",
    type = Boolsk,
    svar = svar,
    ident = ident,
    søknadId = søknadId,
)

fun kanDuJobbeBådeHeltidOgDeltidForReellArbeidssøkerFraQuiz(
    ident: String,
    søknadId: UUID,
    svar: Boolean,
) = QuizOpplysning(
    beskrivendeId = "faktum.jobbe-hel-deltid",
    type = Boolsk,
    svar = svar,
    ident = ident,
    søknadId = søknadId,
)

fun erDuVilligTilÅBytteYrkeEllerGåNedILønnForReellArbeidssøkerFraQuiz(
    ident: String,
    søknadId: UUID,
    svar: Boolean,
) = QuizOpplysning(
    beskrivendeId = "faktum.bytte-yrke-ned-i-lonn",
    type = Boolsk,
    svar = svar,
    ident = ident,
    søknadId = søknadId,
)
