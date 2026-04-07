package no.nav.dagpenger.soknad.orkestrator.søknad.melding

import com.fasterxml.jackson.databind.JsonNode
import io.kotest.assertions.json.shouldContainJsonKey
import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.assertions.json.shouldNotContainJsonKey
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.util.toLowerCasePreservingASCIIRules
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import no.nav.dagpenger.soknad.orkestrator.søknad.Søknad
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonMedTidstempler
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class SøknadEndretTilstandMeldingTest {
    @Test
    fun `test om filtrerSøknadsdataForStatistikk returner riktig seksjonsdata for personalia`() {
        var seksjonsdata =
            @Suppress("ktlint:standard:max-line-length")
            """{"id": "personalia","seksjonsvar": {"fornavnFraPdl": "SOSIAL","mellomnavnFraPdl": "","etternavnFraPdl":"MOLDVARP","fødselsnummerFraPdl": "10928095547","alderFraPdl": "44","adresselinje1FraPdl": "Nygårdsveien 17B","adresselinje2FraPdl": "","adresselinje3FraPdl": "","postnummerFraPdl": "3214","poststedFraPdl": "Sandefjord","landkodeFraPdl": "NO","landFraPdl": "NORGE","kontonummerFraKontoregister": "","folkeregistrertAdresseErNorgeStemmerDet": "nei"},"versjon": 1}"""
        val filtrertPersonalia =
            SøknadEndretTilstandMelding(
                søknadId = UUID.randomUUID(),
                ident = "12345678910",
                forrigeTilstand = "PÅBEGYNT",
                nyTilstand = "INNSENDT",
                søknadsdata =
                    listOf(
                        SeksjonMedTidstempler(
                            seksjonId = "personalia",
                            data = seksjonsdata,
                            opprettet = LocalDateTime.now(),
                            oppdatert = LocalDateTime.now(),
                        ),
                    ),
            ).filtrerSeksjonsdataForStatistikk("personalia", seksjonsdata)

        filtrertPersonalia.shouldNotContainJsonKey("fornavnFraPdl")
        filtrertPersonalia.shouldNotContainJsonKey("mellomnavnFraPdl")
        filtrertPersonalia.shouldContainJsonKey("folkeregistrertAdresseErNorgeStemmerDet")
        filtrertPersonalia.shouldContainJsonKeyValue("folkeregistrertAdresseErNorgeStemmerDet", "nei")
    }

    @Test
    fun `test om filtrerSøknadsdataForStatistikk returner riktig seksjonsdata for arbeidsforhold`() {
        var seksjonsdata =
            @Suppress("ktlint:standard:max-line-length")
            """{"seksjonId":"arbeidsforhold","seksjonsvar":{"hvordanHarDuJobbet":"fastArbeidstidIMindreEnn6Måneder","harDuJobbetIEtAnnetEøsLandSveitsEllerStorbritanniaILøpetAvDeSiste36Månedene":"ja","registrerteArbeidsforhold":[{"navnetPåBedriften":"JOBB AS","hvilketLandJobbetDuI":"SWE","oppgiPersonnummeretPinDuHaddeIDetteLandet":"1234567890","hvordanHarDetteArbeidsforholdetEndretSeg":"jegErPermitert","permittertVarighetPåArbeidsforholdetFraOgMedDato":"2026-03-11","permittertNårErDuPermittertFraOgMedDato":"2026-03-11","permittertNårErDuPermittertTilOgMedDato":"2026-03-25","permittertErDetteEtMidlertidigArbeidsforholdMedEnKontraktfestetSluttdato":"nei","permittertErDuPermittertFraFiskeforedlingsEllerFiskeoljeindustrien":"nei","permittertHvorMangeProsentErDuPermittert":"100","permittertVetDuNårLønnspliktperiodenTilArbeidsgiverenDinEr":"ja","permittertLønnsperiodeFraOgMedDato":"2026-03-17","permittertLønnsperiodeTilOgMedDato":"2026-03-25","harDuJobbetSkiftTurnusEllerRotasjon":"rotasjon","hvilkenTypeRotasjonsordningJobbetDu":"1-1-rotasjon","oppgiSisteArbeidsperiodeIDenSisteRotasjonenDinFraDato":"2026-03-25","oppgiSisteArbeidsperiodeIDenSisteRotasjonenDinTilDato":"2026-03-27","id":"93a32642-6a8b-4339-8040-c3393d02feb2","dokumentasjonskrav":["c91ed639-ac41-437a-9842-15111ef83a0c","c0e41af9-721b-4446-a532-e915429c9b5f","6756643e-ced8-4eeb-8d27-ac891d2d369e"]}]},"versjon":1}"""
        val søknadEndretTilstandMelding =
            SøknadEndretTilstandMelding(
                søknadId = UUID.randomUUID(),
                ident = "12345678910",
                forrigeTilstand = "PÅBEGYNT",
                nyTilstand = "INNSENDT",
                søknadsdata =
                    listOf(
                        SeksjonMedTidstempler(
                            seksjonId = "arbeidsforhold",
                            data = seksjonsdata,
                            opprettet = LocalDateTime.now(),
                            oppdatert = LocalDateTime.now(),
                        ),
                    ),
                pdfGrunnlag =
                    listOf(
                        """{"navn":"Arbeidsforhold","spørsmål":[{"id":"hvordanHarDuJobbet","type":"envalg","label":"Hvilke av de følgende alternativene passer best med hvordan du har jobbet?","options":[{"value":"fastArbeidstidIMindreEnn6Måneder","label":"Jeg har hatt fast arbeidstid i mindre enn seks måneder"},{"value":"fastArbeidstidI6MånederEllerMer","label":"Jeg har hatt fast arbeidstid i seks måneder eller mer"},{"value":"varierendeArbeidstidDeSiste12Månedene","label":"Jeg har hatt varierende arbeidstid de siste 12 månedene"},{"value":"jobbetMerIGjennomsnittDeSiste36MånedeneEnnDeSiste12Månedene","label":"Jeg har jobbet mer i gjennomsnitt de siste 36 månedene enn de siste 12 månedene"},{"value":"harIkkeJobbetDeSiste36Månedene","label":"Jeg har ikke vært i jobb de siste 36 månedene"}],"svar":"fastArbeidstidIMindreEnn6Måneder"},{"id":"lesMerOmArbeidstidLesMer","type":"lesMer","label":"Les mer om arbeidstid","description":"<p>Når vi vurderer om du har rett til dagpenger ser vi på hvor mye du har jobbet,og om .."},{"id":"harDuJobbetIEtAnnetEøsLandSveitsEllerStorbritanniaILøpetAvDeSiste36Månedene","type":"envalg","label":"Har du jobbet i et annet EØS-land,Sveits eller Storbritannia i løpet av de siste 36 månedene?","description":"Andre land i EØS:","options":[{"value":"ja","label":"Ja"},{"value":"nei","label":"Nei"}],"svar":"ja"},{"id":"harDuJobbetIEtAnnetEøsLandSveitsEllerStorbritanniaILøpetAvDeSiste36MånedeneLesMer","type":"lesMer","label":"Grunnen til at vi spør om dette og andre EØS land","description":"Hvis du har jobbet i et annet EØS-land,Sveits eller"},{"id":"harJobbetIEøsOgFastArbeidstidIMindreEnn6MånederForklarendeTekst","type":"forklarendeTekst","description":"<h3>Dine arbeidsforhold</h3>"},[{"id":"navnetPåBedriften","type":"kortTekst","label":"Navnet på bedriften","svar":"JOBB AS"},{"id":"hvilketLandJobbetDuI","type":"land","label":"Hvilket land jobbet du i?","options":[{"value":"NOR","label":"Norge"},{"value":"SWE","label":"Sverige"},{"value":"POL","label":"Polen"}],"svar":"SWE"},{"id":"oppgiPersonnummeretPinDuHaddeIDetteLandet","type":"kortTekst","label":"Oppgi personnummeret (PIN) som du hadde i dette landet","svar":"1234567890"},{"id":"hvordanHarDetteArbeidsforholdetEndretSeg","type":"envalg","label":"Hvordan har dette arbeidsforholdet endret seg?","options":[{"value":"arbeidsgiverenMinHarSagtMegOpp","label":"Arbeidsgiveren min har sagt meg opp"},{"value":"jegHarSagtOppSelv","label":"Jeg har sagt opp selv"},{"value":"jegHarFåttAvskjed","label":"Jeg har fått avskjed"},{"value":"kontraktenErUtgått","label":"Kontrakten er utgått"},{"value":"arbeidstidenErRedusert","label":"Arbeidstiden er redusert"},{"value":"arbeidsgiverErKonkurs","label":"Arbeidsgiver er konkurs"},{"value":"jegErPermitert","label":"Jeg er permittert"},{"value":"arbeidsforholdetErIkkeEndret","label":"Arbeidsforholdet er ikke endret"}],"svar":"jegErPermitert"},{"id":"permittertVarighetPåArbeidsforholdetFraOgMedDato","type":"dato","label":"Når startet du i dette arbeidsforholdet?","svar":"11. mars 2026"},{"id":"permittertNårErDuPermittertFraOgMedDato","type":"periodeFra","label":"Fra og med dato","description":"Hvis du har hatt flere permitteringsperioder skal du oppgi dato for den siste permitteringen.","svar":"11. mars 2026"},{"id":"permittertNårErDuPermittertTilOgMedDato","type":"periodeTil","label":"Til og med dato (valgfritt)","svar":"25. mars 2026"},{"id":"permittertInformasjonskort","type":"informasjonskort","label":"Informasjon","description":"For å ha rett til..."},{"id":"permittertArbeidsavtaleDokumentasjonskravindikator","type":"dokumentasjonskravindikator","label":"Arbeidsavtale"},{"id":"permittertPermiteringsvarselDokumentasjonskravindikator","type":"dokumentasjonskravindikator","label":"Permitteringsvarsel"},{"id":"permittertErDetteEtMidlertidigArbeidsforholdMedEnKontraktfestetSluttdato","type":"envalg","label":"Er du midlertidig ansatt,og har kontrakt med sluttdato?","options":[{"value":"ja","label":"Ja"},{"value":"nei","label":"Nei"},{"value":"vetIkke","label":"Jeg vet ikke"}],"svar":"nei"},{"id":"permittertErDuPermittertFraFiskeforedlingsEllerFiskeoljeindustrien","type":"envalg","label":"Er du permittert fra fiskeforedlings- eller fiskeoljeindustrien?","options":[{"value":"ja","label":"Ja"},{"value":"nei","label":"Nei"}],"svar":"nei"},{"id":"permittertHvorMangeProsentErDuPermittert","type":"tall","label":"Hvor mange prosent er du permittert?","svar":"100"},{"id":"permittertVetDuNårLønnspliktperiodenTilArbeidsgiverenDinEr","type":"envalg","label":"Vet du når lønnspliktperioden til arbeidsgiveren din er?","description":"Du finner informasjon om arbeidsgivers lønnspliktperiode i permitteringsvarselet.","options":[{"value":"ja","label":"Ja"},{"value":"nei","label":"Nei"}],"svar":"ja"},{"id":"permittertLønnsperiodeFraOgMedDato","type":"periodeFra","label":"Fra og med dato","svar":"17. mars 2026"},{"id":"permittertLønnsperiodeTilOgMedDato","type":"periodeTil","label":"Til og med dato (valgfritt)","svar":"25. mars 2026"},{"id":"harDuJobbetSkiftTurnusEllerRotasjon","type":"envalg","label":"Har du jobbet skift,turnus eller rotasjon?","description":"<p>Skift eller","options":[{"value":"skiftEllerTurns","label":"Ja,jeg har jobbet skift eller turnus"},{"value":"rotasjon","label":"Ja,jeg har jobbet rotasjon"},{"value":"hverkenSkiftTurnusEllerRotasjon","label":"Nei,ingen av delene"}],"svar":"rotasjon"},{"id":"harDuJobbetSkiftTurnusEllerRotasjonLesMer","type":"lesMer","label":"Grunnen til at vi spør om dette","description":"<p>Vi må vite hvilken arbeidstidsordning du har for å gi deg dagpenger fra riktig dato.</p>"},{"id":"jegHarJobbetRotasjonDokumentasjonskravindikator","type":"dokumentasjonskravindikator","label":"Dokumentasjon av rotasjonsordningen og den siste arbeidsperioden din"},{"id":"hvilkenTypeRotasjonsordningJobbetDu","type":"envalg","label":"Hvilke type rotasjonsordning jobbet du?","options":[{"value":"2-4-rotasjon","label":"2:4"},{"value":"2-3-rotasjon","label":"2:3"},{"value":"1-1-rotasjon","label":"1:1"},{"value":"annenRotasjon","label":"Annen rotasjon"}],"svar":"1-1-rotasjon"},{"id":"oppgiSisteArbeidsperiodeIDenSisteRotasjonenDinFraDato","type":"periodeFra","label":"Fra dato","svar":"25. mars 2026"},{"id":"oppgiSisteArbeidsperiodeIDenSisteRotasjonenDinTilDato","type":"periodeTil","label":"Til dato","svar":"27. mars 2026"}]]}""",
                        """{"navn":"Personalia","spørsmål":[{"id":"fornavnFraPdl","type":"registeropplysning","label":"Fornavn","svar":"AKTVERDIG"},{"id":"etternavnFraPdl","type":"registeropplysning","label":"Etternavn","svar":"PYRAMIDE"},{"id":"fødselsnummerFraPdl","type":"registeropplysning","label":"Fødselsnummer","svar":"15438643734"},{"id":"adresselinje1FraPdl","type":"registeropplysning","label":"Adresselinje 1","svar":"Gaddevegen 7"},{"id":"postnummerFraPdl","type":"registeropplysning","label":"Postnummer","svar":"6900"},{"id":"poststedFraPdl","type":"registeropplysning","label":"Poststed","svar":"Florø"},{"id":"landkodeFraPdl","type":"registeropplysning","label":"Landkode","svar":"NO"},{"id":"landFraPdl","type":"registeropplysning","label":"Land","svar":"NORGE"},{"id":"personaliaBostedslandForklarendeTekst","type":"forklarendeTekst","description":"<h3>Bostedsland</h3>"},{"id":"folkeregistrertAdresseErNorgeStemmerDet","type":"envalg","label":"Du er folkeregistrert i Norge. Er Norge bostedslandet ditt?","description":"Bostedslandet ditt er det landet du eier eller leier bolig i og tilbringer mesteparten av tiden din, også når du ikke jobber.","options":[{"value":"ja","label":"Ja, Norge er bostedslandet mitt"},{"value":"nei","label":"Nei, Norge er ikke bostedslandet mitt"}],"svar":"ja"}]}""",
                    ),
            )
        val filtrerSpørsmål =
            søknadEndretTilstandMelding.filtrerUaktuelleFelter()
        val filtrertArbeidsforhold =
            søknadEndretTilstandMelding.filtrerSeksjonsdataForStatistikk(
                "arbeidsforhold",
                seksjonsdata,
                filtrerSpørsmål.find { it.seksjonId.toLowerCasePreservingASCIIRules() == "arbeidsforhold" }?.spørsmål ?: emptyList(),
            )

        filtrertArbeidsforhold.shouldContainJsonKey("versjon")
        filtrertArbeidsforhold.shouldContainJsonKey("seksjonsvar")
        filtrertArbeidsforhold.shouldContainJsonKey("seksjonId")

        val seksjonsvarJson = objectMapper.readTree(filtrertArbeidsforhold)
        val seksjonsvarResponse = seksjonsvarJson["seksjonsvar"]

        seksjonsvarResponse["hvordanHarDuJobbet"].asText() shouldBe "fastArbeidstidIMindreEnn6Måneder"
        seksjonsvarResponse["harDuJobbetIEtAnnetEøsLandSveitsEllerStorbritanniaILøpetAvDeSiste36Månedene"].asText() shouldBe "ja"
    }

    @Test
    fun `SøknadEndretTilstandMelding legger til søknadsdata når status går over til innsend med seksjon og søknadsdata sendt med`() {
        @Suppress("ktlint:standard:max-line-length")
        var arbeidsforholdSeksjonsdata =
            """{"seksjonId":"arbeidsforhold","seksjonsvar":{"hvordanHarDuJobbet":"fastArbeidstidIMindreEnn6Måneder","harDuJobbetIEtAnnetEøsLandSveitsEllerStorbritanniaILøpetAvDeSiste36Månedene":"ja","registrerteArbeidsforhold":[{"navnetPåBedriften":"JOBB AS","hvilketLandJobbetDuI":"SWE","oppgiPersonnummeretPinDuHaddeIDetteLandet":"1234567890","hvordanHarDetteArbeidsforholdetEndretSeg":"jegErPermitert","permittertVarighetPåArbeidsforholdetFraOgMedDato":"2026-04-22","permittertNårErDuPermittertFraOgMedDato":"2026-04-07","permittertErDetteEtMidlertidigArbeidsforholdMedEnKontraktfestetSluttdato":"nei","permittertErDuPermittertFraFiskeforedlingsEllerFiskeoljeindustrien":"nei","permittertHvorMangeProsentErDuPermittert":"100","permittertVetDuNårLønnspliktperiodenTilArbeidsgiverenDinEr":"nei","harDuJobbetSkiftTurnusEllerRotasjon":"hverkenSkiftTurnusEllerRotasjon","id":"ac5b861b-99dc-4183-b5c6-de681b51b963","dokumentasjonskrav":["c8c3d436-a9f0-42ca-a10c-6eeee305d350","a8fb5b99-f756-4f82-9c30-7ae68aab4c4a"]},{"navnetPåBedriften":"JOBB 2 AS ","hvilketLandJobbetDuI":"DNK","oppgiPersonnummeretPinDuHaddeIDetteLandet":"1234567890","hvordanHarDetteArbeidsforholdetEndretSeg":"jegHarFåttAvskjed","jegHarFåttAvskjedVarighetPåArbeidsforholdetFraDato":"2026-04-07","jegHarFåttAvskjedVarighetPåArbeidsforholdetTilDato":"2026-04-08","jegHarFåttAvskjedHvaVarÅrsaken":"eee","harDuJobbetSkiftTurnusEllerRotasjon":"hverkenSkiftTurnusEllerRotasjon","id":"814fd3b3-e559-45bf-b881-5c57d2df64f4","dokumentasjonskrav":["372c1dc3-87d5-4bc9-a6a7-f074510a1a5c","74db5550-88ec-41f5-af39-92843361b0ae"]}]},"versjon":1}"""
        var personaliaSeksjonsdata =
            """{"id": "personalia","seksjonsvar": {"fornavnFraPdl": "SOSIAL","mellomnavnFraPdl": "","etternavnFraPdl":"MOLDVARP","fødselsnummerFraPdl": "10928095547","alderFraPdl": "44","adresselinje1FraPdl": "Nygårdsveien 17B","adresselinje2FraPdl": "","adresselinje3FraPdl": "","postnummerFraPdl": "3214","poststedFraPdl": "Sandefjord","landkodeFraPdl": "NO","landFraPdl": "NORGE","kontonummerFraKontoregister": "","folkeregistrertAdresseErNorgeStemmerDet": "nei"},"versjon": 1}"""
        var reellArbeidssøkerData =
            """{"seksjonId":"reell-arbeidssoker","seksjonsvar":{"kanDuJobbeBådeHeltidOgDeltid":"nei","kanIkkeJobbeHeltidOgDeltidSituasjonenSomGjelderDeg":["kanIkkeJobbeHeltidOgDeltidOmsorgForBarnUnderEttÅr","kanIkkeJobbeHeltidOgDeltidHarFylt60"],"kanIkkeJobbeBådeHeltidOgDeltidAntallTimer":"12","kanDuJobbeIHeleNorge":"nei","kanIkkeJobbeIHeleNorgeSituasjonenSomGjelderDeg":["kanIkkeJobbeIHeleNorgeOmsorgForPleietrengendeINærFamilie","kanIkkeJobbeIHeleNorgeEneansvarEllerDeltAnsvarForBarnTilOgMed7Klasse","kanIkkeJobbeIHeleNorgeEneansvarEllerDeltAnsvarForBarnUnder18ÅrMedSpesielleBehov"],"kanDuTaAlleTyperArbeid":"nei","kanDuTaAlleTyperArbeidHvilkeTyperArbeidKanDuIkkeTa":"gg","erDuVilligTilÅBytteYrkeEllerGåNedILønn":"ja","dokumentasjonskrav":"[{\"id\":\"d4209d5e-eadf-40f0-95e8-8285f37fd5ed\",\"seksjonId\":\"reell-arbeidssoker\",\"spørsmålId\":\"kanIkkeJobbeIHeleNorgeSituasjonenSomGjelderDeg\",\"skjemakode\":\"T9\",\"tittel\":\"Bekreftelse fra relevant fagpersonell fordi du ikke kan jobbe i hele Norge\",\"type\":\"ReellArbeidssøkerKanIkkeJobbeHeleNorge\"},{\"id\":\"03d427df-f8c1-4881-a570-ef325d491e78\",\"seksjonId\":\"reell-arbeidssoker\",\"spørsmålId\":\"kanDuTaAlleTyperArbeid\",\"skjemakode\":\"T9\",\"tittel\":\"Bekreftelse fra lege eller annen behandler fordi du ikke kan ta alle typer arbeid\",\"type\":\"ReellArbeidssøkerKanIkkeTaAlleTyperArbeid\"}]"},"versjon":1}"""
        val søknadEndretTilstandMelding =
            SøknadEndretTilstandMelding(
                søknadId = UUID.randomUUID(),
                ident = "12345678910",
                forrigeTilstand = "PÅBEGYNT",
                nyTilstand = "INNSENDT",
                søknadsdata =
                    listOf(
                        SeksjonMedTidstempler(
                            seksjonId = "arbeidsforhold",
                            data = arbeidsforholdSeksjonsdata,
                            opprettet = LocalDateTime.now(),
                            oppdatert = LocalDateTime.now(),
                        ),
                        SeksjonMedTidstempler(
                            seksjonId = "personalia",
                            data = personaliaSeksjonsdata,
                            opprettet = LocalDateTime.now(),
                            oppdatert = LocalDateTime.now(),
                        ),
                        SeksjonMedTidstempler(
                            seksjonId = "reell-arbeidssoker",
                            data = reellArbeidssøkerData,
                            opprettet = LocalDateTime.now(),
                            oppdatert = LocalDateTime.now(),
                        ),
                    ),
                pdfGrunnlag =
                    listOf(
                        """{"navn":"Arbeidsforhold","spørsmål":[{"id":"hvordanHarDuJobbet","type":"envalg","label":"Hvilke av de følgende alternativene passer best med hvordan du har jobbet?","options":[{"value":"fastArbeidstidIMindreEnn6Måneder","label":"Jeg har hatt fast arbeidstid i mindre enn seks måneder"},{"value":"fastArbeidstidI6MånederEllerMer","label":"Jeg har hatt fast arbeidstid i seks måneder eller mer"},{"value":"varierendeArbeidstidDeSiste12Månedene","label":"Jeg har hatt varierende arbeidstid de siste 12 månedene"},{"value":"jobbetMerIGjennomsnittDeSiste36MånedeneEnnDeSiste12Månedene","label":"Jeg har jobbet mer i gjennomsnitt de siste 36 månedene enn de siste 12 månedene"},{"value":"harIkkeJobbetDeSiste36Månedene","label":"Jeg har ikke vært i jobb de siste 36 månedene"}],"svar":"fastArbeidstidIMindreEnn6Måneder"},{"id":"lesMerOmArbeidstidLesMer","type":"lesMer","label":"Les mer om arbeidstid","description":"<p>Når vi vurderer om du har rett til dagpenger</p>"},{"id":"harDuJobbetIEtAnnetEøsLandSveitsEllerStorbritanniaILøpetAvDeSiste36Månedene","type":"envalg","label":"Har du jobbet i et annet EØS-land,Sveits eller Storbritannia i løpet av de siste 36 månedene?","description":"Andre land i EØS:Belgia,Bulgaria,Danmark,Estland...","options":[{"value":"ja","label":"Ja"},{"value":"nei","label":"Nei"}],"svar":"ja"},{"id":"harDuJobbetIEtAnnetEøsLandSveitsEllerStorbritanniaILøpetAvDeSiste36MånedeneLesMer","type":"lesMer","label":"Grunnen til at vi spør om dette og andre EØS land","description":"Hvis du har jobbet i et annet EØS-land,Sveits eller Storbritannia kan du ha rett til å"},{"id":"harJobbetIEøsOgFastArbeidstidIMindreEnn6MånederForklarendeTekst","type":"forklarendeTekst","description":"<h3>Dine arbeidsforhold</h3>"},[{"id":"navnetPåBedriften","type":"kortTekst","label":"Navnet på bedriften","svar":"JOBB AS"},{"id":"hvilketLandJobbetDuI","type":"land","label":"Hvilket land jobbet du i?","options":[{"value":"NOR","label":"Norge"},{"value":"SWE","label":"Sverige"},{"value":"POL","label":"Polen"}],"svar":"SWE"},{"id":"oppgiPersonnummeretPinDuHaddeIDetteLandet","type":"kortTekst","label":"Oppgi personnummeret (PIN) som du hadde i dette landet","svar":"1234567890"},{"id":"hvordanHarDetteArbeidsforholdetEndretSeg","type":"envalg","label":"Hvordan har dette arbeidsforholdet endret seg?","options":[{"value":"arbeidsgiverenMinHarSagtMegOpp","label":"Arbeidsgiveren min har sagt meg opp"},{"value":"jegHarSagtOppSelv","label":"Jeg har sagt opp selv"},{"value":"jegHarFåttAvskjed","label":"Jeg har fått avskjed"},{"value":"kontraktenErUtgått","label":"Kontrakten er utgått"},{"value":"arbeidstidenErRedusert","label":"Arbeidstiden er redusert"},{"value":"arbeidsgiverErKonkurs","label":"Arbeidsgiver er konkurs"},{"value":"jegErPermitert","label":"Jeg er permittert"},{"value":"arbeidsforholdetErIkkeEndret","label":"Arbeidsforholdet er ikke endret"}],"svar":"jegErPermitert"},{"id":"permittertVarighetPåArbeidsforholdetFraOgMedDato","type":"dato","label":"Når startet du i dette arbeidsforholdet?","svar":"22. april 2026"},{"id":"permittertNårErDuPermittertFraOgMedDato","type":"periodeFra","label":"Fra og med dato","description":"Hvis du har hatt flere permitteringsperioder skal du oppgi dato for den siste permitteringen.","svar":"7. april 2026"},{"id":"permittertNårErDuPermittertTilOgMedDato","type":"periodeTil","label":"Til og med dato (valgfritt)"},{"id":"permittertInformasjonskort","type":"informasjonskort","label":"Informasjon","description":"For å ha rett til dagpenger..."},{"id":"permittertArbeidsavtaleDokumentasjonskravindikator","type":"dokumentasjonskravindikator","label":"Arbeidsavtale"},{"id":"permittertPermiteringsvarselDokumentasjonskravindikator","type":"dokumentasjonskravindikator","label":"Permitteringsvarsel"},{"id":"permittertErDetteEtMidlertidigArbeidsforholdMedEnKontraktfestetSluttdato","type":"envalg","label":"Er du midlertidig ansatt,og har kontrakt med sluttdato?","options":[{"value":"ja","label":"Ja"},{"value":"nei","label":"Nei"},{"value":"vetIkke","label":"Jeg vet ikke"}],"svar":"nei"},{"id":"permittertErDuPermittertFraFiskeforedlingsEllerFiskeoljeindustrien","type":"envalg","label":"Er du permittert fra fiskeforedlings- eller fiskeoljeindustrien?","options":[{"value":"ja","label":"Ja"},{"value":"nei","label":"Nei"}],"svar":"nei"},{"id":"permittertHvorMangeProsentErDuPermittert","type":"tall","label":"Hvor mange prosent er du permittert?","svar":"100"},{"id":"permittertVetDuNårLønnspliktperiodenTilArbeidsgiverenDinEr","type":"envalg","label":"Vet du når lønnspliktperioden til arbeidsgiveren din er?","description":"Du finner informasjon om arbeidsgivers lønnspliktperiode ","options":[{"value":"ja","label":"Ja"},{"value":"nei","label":"Nei"}],"svar":"nei"},{"id":"harDuJobbetSkiftTurnusEllerRotasjon","type":"envalg","label":"Har du jobbet skift,turnus eller rotasjon?","description":"<p>Skift eller turnus kan være når du</p>","options":[{"value":"skiftEllerTurns","label":"Ja,jeg har jobbet skift eller turnus"},{"value":"rotasjon","label":"Ja,jeg har jobbet rotasjon"},{"value":"hverkenSkiftTurnusEllerRotasjon","label":"Nei,ingen av delene"}],"svar":"hverkenSkiftTurnusEllerRotasjon"},{"id":"harDuJobbetSkiftTurnusEllerRotasjonLesMer","type":"lesMer","label":"Grunnen til at vi spør om dette","description":"<p>Vi må vite hvilken arbeidstidsordning du har for å gi deg dagpenger fra riktig dato.</p>"}],[{"id":"navnetPåBedriften","type":"kortTekst","label":"Navnet på bedriften","svar":"JOBB 2 AS "},{"id":"hvilketLandJobbetDuI","type":"land","label":"Hvilket land jobbet du i?","options":[{"value":"NOR","label":"Norge"},{"value":"SWE","label":"Sverige"},{"value":"POL","label":"Polen"}],"svar":"DNK"},{"id":"oppgiPersonnummeretPinDuHaddeIDetteLandet","type":"kortTekst","label":"Oppgi personnummeret (PIN) som du hadde i dette landet","svar":"1234567890"},{"id":"hvordanHarDetteArbeidsforholdetEndretSeg","type":"envalg","label":"Hvordan har dette arbeidsforholdet endret seg?","options":[{"value":"arbeidsgiverenMinHarSagtMegOpp","label":"Arbeidsgiveren min har sagt meg opp"},{"value":"jegHarSagtOppSelv","label":"Jeg har sagt opp selv"},{"value":"jegHarFåttAvskjed","label":"Jeg har fått avskjed"},{"value":"kontraktenErUtgått","label":"Kontrakten er utgått"},{"value":"arbeidstidenErRedusert","label":"Arbeidstiden er redusert"},{"value":"arbeidsgiverErKonkurs","label":"Arbeidsgiver er konkurs"},{"value":"jegErPermitert","label":"Jeg er permittert"},{"value":"arbeidsforholdetErIkkeEndret","label":"Arbeidsforholdet er ikke endret"}],"svar":"jegHarFåttAvskjed"},{"id":"jegHarFåttAvskjedVarighetPåArbeidsforholdetFraDato","type":"periodeFra","label":"Fra dato","svar":"7. april 2026"},{"id":"jegHarFåttAvskjedVarighetPåArbeidsforholdetTilDato","type":"periodeTil","label":"Til dato","svar":"8. april 2026"},{"id":"jegHarFåttAvskjedInformasjonskort","type":"informasjonskort","label":"Informasjon","description":"<p>Hvis du har fått avskjed fra arbeidsgiver,må ...</p>"},{"id":"jegHarFåttAvskjedArbeidsavtaleDokumentasjonskravindikator","type":"dokumentasjonskravindikator","label":"Arbeidsavtale"},{"id":"jegHarFåttAvskjedDokumentasjonskravindikator","type":"dokumentasjonskravindikator","label":"Avskjedigelse"},{"id":"jegHarFåttAvskjedHvaVarÅrsaken","type":"langTekst","label":"Hva var årsaken til at du ble avskjediget?","svar":"eee"},{"id":"harDuJobbetSkiftTurnusEllerRotasjon","type":"envalg","label":"Har du jobbet skift,turnus eller rotasjon?","description":"<p>Skift eller turnus kan være når...</p>","options":[{"value":"skiftEllerTurns","label":"Ja,jeg har jobbet skift eller turnus"},{"value":"rotasjon","label":"Ja,jeg har jobbet rotasjon"},{"value":"hverkenSkiftTurnusEllerRotasjon","label":"Nei,ingen av delene"}],"svar":"hverkenSkiftTurnusEllerRotasjon"},{"id":"harDuJobbetSkiftTurnusEllerRotasjonLesMer","type":"lesMer","label":"Grunnen til at vi spør om dette","description":"<p>Vi må vite hvilken arbeidstidsordning du har for å gi deg dagpenger fra riktig dato.</p>"}]]}""",
                        """{"navn":"Personalia","spørsmål":[{"id":"fornavnFraPdl","type":"registeropplysning","label":"Fornavn","svar":"AKTVERDIG"},{"id":"etternavnFraPdl","type":"registeropplysning","label":"Etternavn","svar":"PYRAMIDE"},{"id":"fødselsnummerFraPdl","type":"registeropplysning","label":"Fødselsnummer","svar":"15438643734"},{"id":"adresselinje1FraPdl","type":"registeropplysning","label":"Adresselinje 1","svar":"Gaddevegen 7"},{"id":"postnummerFraPdl","type":"registeropplysning","label":"Postnummer","svar":"6900"},{"id":"poststedFraPdl","type":"registeropplysning","label":"Poststed","svar":"Florø"},{"id":"landkodeFraPdl","type":"registeropplysning","label":"Landkode","svar":"NO"},{"id":"landFraPdl","type":"registeropplysning","label":"Land","svar":"NORGE"},{"id":"personaliaBostedslandForklarendeTekst","type":"forklarendeTekst","description":"<h3>Bostedsland</h3>"},{"id":"folkeregistrertAdresseErNorgeStemmerDet","type":"envalg","label":"Du er folkeregistrert i Norge. Er Norge bostedslandet ditt?","description":"Bostedslandet ditt er det landet du eier eller leier bolig i og tilbringer mesteparten av tiden din, også når du ikke jobber.","options":[{"value":"ja","label":"Ja, Norge er bostedslandet mitt"},{"value":"nei","label":"Nei, Norge er ikke bostedslandet mitt"}],"svar":"ja"}]}""",
                        """{"navn":"Reell arbeidssøker","spørsmål":[{"id":"reellArbeidssøkerForklarendeTekst","type":"forklarendeTekst","description":"<p>For å få dagpenger må du være reell arbeidssøker."},{"id":"kanDuTaAlleTyperArbeid","type":"envalg","label":"Kan du ta alle typer arbeid?","description":"Hvis du har redusert helse og ikke kan ta alle typer arbeid,kan vi ta hensyn til dette.","options":[{"value":"ja","label":"Ja"},{"value":"nei","label":"Nei"}],"svar":"nei"},{"id":"kanDuTaAlleTyperArbeidLesMer","type":"lesMer","label":"Dette regnes som redusert helse","description":"Med redusert helse mener vi for eksempel"},{"id":"kanDuTaAlleTyperArbeidInformasjonskort","type":"informasjonskort","label":"Du kan få avslag på søknaden","description":"<p>For å ha rett til...</p>"},{"id":"kanDuTaAlleTyperArbeidHvilkeTyperArbeidKanDuIkkeTa","type":"langTekst","label":"Hvilke typer arbeid kan du ikke ta?","svar":"gg"},{"id":"kanDuTaAlleTyperArbeidHvilkeTyperArbeidKanDuIkkeTaDokumentasjonskravindikator","type":"dokumentasjonskravindikator","label":"Bekreftelse fra lege eller annen behandler fordi du ikke kan ta alle typer arbeid"},{"id":"erDuVilligTilÅBytteYrkeEllerGåNedILønn","type":"envalg","label":"Er du villig til å bytte yrke eller gå ned i lønn?","description":"For å ha rett til dagpenger må du.","options":[{"value":"ja","label":"Ja"},{"value":"nei","label":"Nei"}],"svar":"ja"},{"id":"kanDuJobbeBådeHeltidOgDeltid","type":"envalg","label":"Kan du jobbe både heltid og deltid?","description":"Som hovedregel må du kunne ta både..","options":[{"value":"ja","label":"Ja"},{"value":"nei","label":"Nei"}],"svar":"nei"},{"id":"kanIkkeJobbeHeltidOgDeltidSituasjonenSomGjelderDeg","type":"flervalg","label":"Velg årsaken som gjelder deg","description":"Du kan krysse av for flere alternativer. Med...","options":[{"value":"kanIkkeJobbeHeltidOgDeltidRedusertHelse","label":"Redusert helse,fysisk eller psykisk"},{"value":"kanIkkeJobbeHeltidOgDeltidOmsorgForBarnUnderEttÅr","label":"Omsorg for barn under ett år"},{"value":"kanIkkeJobbeHeltidOgDeltidOmsorgForPleietrengendeINærFamilie","label":"Omsorgsansvar for pleietrengende i nær familie,for eksempel barn,foreldre eller ektefelle"},{"value":"kanIkkeJobbeHeltidOgDeltidEneansvarEllerDeltAnsvarForBarnTilOgMed7Klasse","label":"Eneansvar eller delt ansvar med en annen forelder som du ikke bor sammen med,for barn til og med 7. klasse"},{"value":"kanIkkeJobbeHeltidOgDeltidEneansvarEllerDeltAnsvarForBarnUnder18ÅrMedSpesielleBehov","label":"Eneansvar eller delt ansvar med en annen forelder som du ikke bor sammen med,for barn under 18 år med spesielle behov"},{"value":"kanIkkeJobbeHeltidOgDeltidDenAndreForeldrenJobberSkiftEllerLignendeOgAnsvarForBarnTilOgMed7KlasseEllerMedSpesielleBehov","label":"Den andre forelderen jobber skift,turnus eller utenfor nærområdet. Og "},{"value":"kanIkkeJobbeHeltidOgDeltidJegErPermitert","label":"Jeg er permittert"},{"value":"kanIkkeJobbeHeltidOgDeltidHarFylt60","label":"Har fylt 60 år"},{"value":"kanIkkeJobbeHeltidOgDeltidAnnenSituasjon","label":"Annen situasjon"}],"svar":["kanIkkeJobbeHeltidOgDeltidOmsorgForBarnUnderEttÅr","kanIkkeJobbeHeltidOgDeltidHarFylt60"]},{"id":"kanIkkeJobbeHeltidOgDeltidHarFylt60Informasjonskort","type":"informasjonskort","label":"Informasjon","description":"<p>Siden du er over"},{"id":"kanIkkeJobbeBådeHeltidOgDeltidAntallTimer","type":"tall","label":"Skriv inn antall timer du kan jobbe per uke","description":"For å få rett til dagpenger må du normalt kunne jobbe minst 18,75 timer per uke.","svar":"12"},{"id":"kanIkkeJobbeBådeHeltidOgDeltidAntallTimerLesMer","type":"lesMer","label":"Hvis du har uføretrygd","description":"Hvis du har uføretrygd må du kunne jobbe minst 11,25 timer per uke."},{"id":"kanDuJobbeIHeleNorge","type":"envalg","label":"Kan du jobbe i hele Norge?","description":"Som hovedregel må du være villig til å ta arbeid i hele Norge for å ha rett til dagpenger.","options":[{"value":"ja","label":"Ja"},{"value":"nei","label":"Nei"}],"svar":"nei"},{"id":"kanIkkeJobbeIHeleNorgeSituasjonenSomGjelderDeg","type":"flervalg","label":"Velg årsaken som gjelder deg","description":"Du kan krysse av for flere alternativer. Med mindre Nav har opplysningene allerede,så må du dokumentere årsaken.","options":[{"value":"kanIkkeJobbeIHeleNorgeRedusertHelse","label":"Redusert helse,fysisk eller psykisk"},{"value":"kanIkkeJobbeIHeleNorgeOmsorgForBarnUnderEttÅr","label":"Omsorg for barn under ett år"},{"value":"kanIkkeJobbeIHeleNorgeOmsorgForPleietrengendeINærFamilie","label":"Omsorgsansvar for pleietrengende i nær familie,for eksempel barn,foreldre eller ektefelle"},{"value":"kanIkkeJobbeIHeleNorgeEneansvarEllerDeltAnsvarForBarnTilOgMed7Klasse","label":"Eneansvar eller delt ansvar med en annen forelder som du ikke bor sammen med,for barn til og med 7. klasse"},{"value":"kanIkkeJobbeIHeleNorgeEneansvarEllerDeltAnsvarForBarnUnder18ÅrMedSpesielleBehov","label":"Eneansvar eller delt ansvar med en annen forelder som du ikke bor sammen med,for barn under 18 år med spesielle behov"},{"value":"kanIkkeJobbeIHeleNorgeDenAndreForeldrenJobberSkiftEllerLignendeOgAnsvarForBarnTilOgMed7KlasseEllerMedSpesielleBehov","label":"Den andre forelderen jobber skift,turnus eller utenfor nærområdet,og dere har ansvar for barn til og med 7. klasse eller barn under 18 år med spesielle behov"},{"value":"kanIkkeJobbeIHeleNorgeJegErPermitert","label":"Jeg er permittert"},{"value":"kanIkkeJobbeIHeleNorgeHarFylt60","label":"Har fylt 60 år"},{"value":"kanIkkeJobbeIHeleNorgeAnnenSituasjon","label":"Annen situasjon"}],"svar":["kanIkkeJobbeIHeleNorgeOmsorgForPleietrengendeINærFamilie","kanIkkeJobbeIHeleNorgeEneansvarEllerDeltAnsvarForBarnTilOgMed7Klasse","kanIkkeJobbeIHeleNorgeEneansvarEllerDeltAnsvarForBarnUnder18ÅrMedSpesielleBehov"]},{"id":"kanIkkeJobbeIHeleNorgeSitasjonenSomGjelderDegLesMer","type":"lesMer","label":"Hvis ingen av disse situasjonene passer deg","description":"Hvis du har svart nei og ingen av disse situasjonene gjelder for deg,kan du få avslag på"},{"id":"kanIkkeJobbeIHeleNorgeDokumentasjonskravindikator","type":"dokumentasjonskravindikator","label":"Bekreftelse fra relevant fagpersonell fordi du ikke kan jobbe i hele Norge"}]}""",
                    ),
                søknad =
                    Søknad(
                        ident = "12345678910",
                        oppdatertTidspunkt = LocalDateTime.now(),
                        innsendtTidspunkt = LocalDateTime.now(),
                    ),
            ).asMessage()

        var meldingJson = søknadEndretTilstandMelding.toJson()
        meldingJson.shouldContainJsonKey("søknadsdata")

        var søknadsdataJson = objectMapper.readTree(meldingJson)
        søknadsdataJson["søknadsdata"]["personalia"].shouldNotBeNull()
        søknadsdataJson["søknadsdata"]["arbeidsforhold"].shouldNotBeNull()
        søknadsdataJson["søknadsdata"]["reell-arbeidssoker"].shouldNotBeNull()
        søknadsdataJson["søknadsdata"]["utdanning"].shouldBeNull()

        val arbeidsforholdSeksjonsvar = hentSøknadsdataSomJson(søknadsdataJson, "arbeidsforhold")?.get("seksjonsvar")!!
        arbeidsforholdSeksjonsvar["hvordanHarDuJobbet"].shouldNotBeNull()
        arbeidsforholdSeksjonsvar["registrerteArbeidsforhold"].shouldNotBeNull()
        arbeidsforholdSeksjonsvar["registrerteArbeidsforhold"]["navnetPåBedriften"].shouldBeNull()

        val reellArbeidssøkerSeksjonsvar = hentSøknadsdataSomJson(søknadsdataJson, "reell-arbeidssoker")?.get("seksjonsvar")!!
        reellArbeidssøkerSeksjonsvar["kanDuJobbeBådeHeltidOgDeltid"].shouldNotBeNull()
        reellArbeidssøkerSeksjonsvar["kanDuTaAlleTyperArbeidHvilkeTyperArbeidKanDuIkkeTa"].shouldBeNull()

        val personaliaSeksjonsvar = hentSøknadsdataSomJson(søknadsdataJson, "personalia")?.get("seksjonsvar")!!
        personaliaSeksjonsvar["folkeregistrertAdresseErNorgeStemmerDet"].shouldNotBeNull()
        personaliaSeksjonsvar["fornavnFraPdl"].shouldBeNull()
    }

    @Test
    fun `SøknadEndretTilstandMelding ikke legger til søknadsdata når status går over til påbegynt`() {
        val søknadEndretTilstandMelding =
            SøknadEndretTilstandMelding(
                søknadId = UUID.randomUUID(),
                ident = "12345678910",
                forrigeTilstand = "UnderOpprettelse",
                nyTilstand = "PÅBEGYNT",
            ).asMessage()

        søknadEndretTilstandMelding.toJson().shouldNotContainJsonKey("søknadsdata")
    }

    private fun hentSøknadsdataSomJson(
        søknadsdataJson: JsonNode,
        seksjonId: String,
    ): JsonNode? = objectMapper.readTree(søknadsdataJson["søknadsdata"][seksjonId]["seksjonsdata"].asText())
}
