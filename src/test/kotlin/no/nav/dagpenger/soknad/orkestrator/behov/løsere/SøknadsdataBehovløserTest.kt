package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.soknad.orkestrator.behov.FellesBehovløserLøsninger
import no.nav.dagpenger.soknad.orkestrator.behov.annenPengestøtteOrkestratorJson
import no.nav.dagpenger.soknad.orkestrator.behov.arbeidsforholdMedRegistrerteAvsluttedeArbeidsforholdOrkestratorJson
import no.nav.dagpenger.soknad.orkestrator.behov.arbeidsforholdUtenRegistrerteAvsluttedeArbeidsforholdOrkestratorJson
import no.nav.dagpenger.soknad.orkestrator.behov.avtjentVernepliktOrkestratorJson
import no.nav.dagpenger.soknad.orkestrator.behov.barnetilleggMedBarnFraPdlOgManueltLagteBarn
import no.nav.dagpenger.soknad.orkestrator.behov.barnetilleggMedBarnLagtManuelUtenBarnFraPdl
import no.nav.dagpenger.soknad.orkestrator.behov.barnetilleggMedToBarnFraPdlOgUtenManuelLagteBarn
import no.nav.dagpenger.soknad.orkestrator.behov.barnetilleggUtenBarn
import no.nav.dagpenger.soknad.orkestrator.behov.dinSituasjonMedDagpengerFraDato
import no.nav.dagpenger.soknad.orkestrator.behov.dinSituasjonMedGjenopptakelseOrkestratorJson
import no.nav.dagpenger.soknad.orkestrator.behov.erDuVilligTilÅBytteYrkeEllerGåNedILønnForReellArbeidssøkerFraQuiz
import no.nav.dagpenger.soknad.orkestrator.behov.eøsArbeidsforholdOrkestratorJson
import no.nav.dagpenger.soknad.orkestrator.behov.hentAlleTypeArbeidForReellArbeidssøkerFraQuiz
import no.nav.dagpenger.soknad.orkestrator.behov.hentKanDuJobbeIHeleNorgeForReellArbeidssøkerFraQuiz
import no.nav.dagpenger.soknad.orkestrator.behov.kanDuJobbeBådeHeltidOgDeltidForReellArbeidssøkerFraQuiz
import no.nav.dagpenger.soknad.orkestrator.behov.manuelLagteBarnFraQuiz
import no.nav.dagpenger.soknad.orkestrator.behov.pdlBarnFraQuiz
import no.nav.dagpenger.soknad.orkestrator.behov.personaliaOrkestratorJson
import no.nav.dagpenger.soknad.orkestrator.behov.reellArbeidssøkerOrkestratorJson
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.QuizOpplysning
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Arbeidsforhold
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.ArbeidsforholdSvar
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Barn
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Boolsk
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Dato
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Sluttårsak
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Tekst
import no.nav.dagpenger.soknad.orkestrator.søknad.Søknad
import no.nav.dagpenger.soknad.orkestrator.søknad.Tilstand
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonRepository
import no.nav.dagpenger.soknad.orkestrator.utils.InMemoryQuizOpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.utils.asUUID
import org.junit.jupiter.api.BeforeEach
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.test.Test

class SøknadsdataBehovløserTest {
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
        SøknadsdataBehovløser(
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
                any(),
                "din-situasjon",
            )
        } returns dinSituasjonMedGjenopptakelseOrkestratorJson(now)

        every {
            seksjonRepository.hentSeksjonsvarEllerKastException(
                any(),
                any(),
                "verneplikt",
            )
        } returns avtjentVernepliktOrkestratorJson("ja")
    }

    @Test
    fun `EøsBostedsland fra orkestrator søknad`() {
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

            val søknadstidspunkt = ZonedDateTime.now()
            every {
                søknadRepository.hent(any())
            } returns
                Søknad(
                    søknadId = søknadId,
                    ident = ident,
                    tilstand = Tilstand.INNSENDT,
                    innsendtTidspunkt = søknadstidspunkt.toLocalDateTime(),
                )

            behovløser.løs(lagBehovmeldingUtenSøknadId(ident))

            testRapid.inspektør.message(0)["@løsning"]["Søknadsdata"].also { løsning ->
                val verdi = løsning["verdi"]
                verdi["eøsBostedsland"].asBoolean() shouldBe expectedEøsBostedsland
                verdi["søknadId"].asUUID() shouldBe søknadId
                verdi["ønskerDagpengerFraDato"].asLocalDate() shouldBe now
                løsning["gjelderFra"].asLocalDate() shouldBe søknadstidspunkt.toLocalDate()
            }
            testRapid.reset()
        }
    }

    @Test
    fun `EøsBostedsland fra quiz søknad`() {
        val cases =
            listOf(
                Pair("NOR", false),
                Pair("SWE", true),
                Pair("BRA", false),
            )
        cases.forEach { (bostedsland, expectedEøsBostedsland) ->
            val quizSøknadId = UUID.randomUUID()

            every {
                søknadRepository.hentSøknadIdFraJournalPostId(any(), any())
            } returns quizSøknadId

            val opplysning =
                QuizOpplysning(
                    beskrivendeId = "faktum.hvilket-land-bor-du-i",
                    type = Tekst,
                    svar = bostedsland,
                    ident = ident,
                    søknadId = quizSøknadId,
                )
            val søknadstidspunkt = ZonedDateTime.now()
            val søknadstidpsunktOpplysning =
                QuizOpplysning(
                    beskrivendeId = "søknadstidspunkt",
                    type = Tekst,
                    svar = søknadstidspunkt.toString(),
                    ident = ident,
                    søknadId = quizSøknadId,
                )
            opplysningRepository.lagre(opplysning)
            opplysningRepository.lagre(søknadstidpsunktOpplysning)

            behovløser.løs(lagBehovmeldingUtenSøknadId(ident))

            testRapid.inspektør.message(0)["@løsning"]["Søknadsdata"].also { løsning ->
                val verdi = løsning["verdi"]
                verdi["eøsBostedsland"].asBoolean() shouldBe expectedEøsBostedsland
                verdi["søknadId"].asUUID() shouldBe quizSøknadId
                verdi["ønskerDagpengerFraDato"].asLocalDate() shouldBe now
                løsning["gjelderFra"].asLocalDate() shouldBe søknadstidspunkt.toLocalDate()
            }
            testRapid.reset()
        }
    }

    @Test
    fun `EøsArbeidsforhold orkestrator søknad`() {
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

            val søknadstidspunkt = ZonedDateTime.now()
            every {
                søknadRepository.hent(any())
            } returns
                Søknad(
                    søknadId = søknadId,
                    ident = ident,
                    tilstand = Tilstand.INNSENDT,
                    innsendtTidspunkt = søknadstidspunkt.toLocalDateTime(),
                )

            behovløser.løs(lagBehovmeldingUtenSøknadId(ident))

            testRapid.inspektør.message(0)["@løsning"]["Søknadsdata"].also { løsning ->
                val verdi = løsning["verdi"]
                verdi["eøsArbeidsforhold"].asBoolean() shouldBe expectedEøsArbeidsforhold
                verdi["søknadId"].asUUID() shouldBe søknadId
                verdi["ønskerDagpengerFraDato"].asLocalDate() shouldBe now
                løsning["gjelderFra"].asLocalDate() shouldBe søknadstidspunkt.toLocalDate()
            }
            testRapid.reset()
        }
    }

    @Test
    fun `EøsArbeidsforhold quiz søknad`() {
        val cases =
            listOf(
                Pair(true, true),
                Pair(false, false),
            )
        cases.forEach { (eøsArbeidsforhold, expectedEøsArbeidsforhold) ->
            val quizSøknadId = UUID.randomUUID()

            every {
                søknadRepository.hentSøknadIdFraJournalPostId(any(), any())
            } returns quizSøknadId

            val opplysning =
                QuizOpplysning(
                    beskrivendeId = "faktum.eos-arbeid-siste-36-mnd",
                    type = Boolsk,
                    svar = eøsArbeidsforhold,
                    ident = ident,
                    søknadId = quizSøknadId,
                )
            val søknadstidspunkt = ZonedDateTime.now()
            val søknadstidpsunktOpplysning =
                QuizOpplysning(
                    beskrivendeId = "søknadstidspunkt",
                    type = Tekst,
                    svar = søknadstidspunkt.toString(),
                    ident = ident,
                    søknadId = quizSøknadId,
                )
            opplysningRepository.lagre(opplysning)
            opplysningRepository.lagre(søknadstidpsunktOpplysning)

            behovløser.løs(lagBehovmeldingUtenSøknadId(ident))

            testRapid.inspektør.message(0)["@løsning"]["Søknadsdata"].also { løsning ->
                val verdi = løsning["verdi"]
                verdi["eøsArbeidsforhold"].asBoolean() shouldBe expectedEøsArbeidsforhold
                verdi["søknadId"].asUUID() shouldBe quizSøknadId
                verdi["ønskerDagpengerFraDato"].asLocalDate() shouldBe now
                løsning["gjelderFra"].asLocalDate() shouldBe søknadstidspunkt.toLocalDate()
            }
            testRapid.reset()
        }
    }

    @Test
    fun `AvtjentVerneplikt orkestrator søknad`() {
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

            val søknadstidspunkt = ZonedDateTime.now()
            every {
                søknadRepository.hent(any())
            } returns
                Søknad(
                    søknadId = søknadId,
                    ident = ident,
                    tilstand = Tilstand.INNSENDT,
                    innsendtTidspunkt = søknadstidspunkt.toLocalDateTime(),
                )

            behovløser.løs(lagBehovmeldingUtenSøknadId(ident))
            testRapid.inspektør.message(0)["@løsning"]["Søknadsdata"].also { løsning ->
                val verdi = løsning["verdi"]
                verdi["avtjentVerneplikt"].asBoolean() shouldBe expectedAvtjentVerneplikt
                verdi["søknadId"].asUUID() shouldBe søknadId
                verdi["ønskerDagpengerFraDato"].asLocalDate() shouldBe now
                løsning["gjelderFra"].asLocalDate() shouldBe søknadstidspunkt.toLocalDate()
            }
            testRapid.reset()
        }
    }

    @Test
    fun `AvtjentVerneplikt for quiz søknad`() {
        val cases =
            listOf(
                Pair(true, true),
                Pair(false, false),
            )
        cases.forEach { (avtjentVerneplikt, expectedAvtjentVerneplikt) ->
            val quizSøknadId = UUID.randomUUID()

            every {
                søknadRepository.hentSøknadIdFraJournalPostId(any(), any())
            } returns quizSøknadId

            val opplysning =
                QuizOpplysning(
                    beskrivendeId = "faktum.avtjent-militaer-sivilforsvar-tjeneste-siste-12-mnd",
                    type = Boolsk,
                    svar = avtjentVerneplikt,
                    ident = ident,
                    søknadId = quizSøknadId,
                )
            val søknadstidspunkt = ZonedDateTime.now()
            val søknadstidpsunktOpplysning =
                QuizOpplysning(
                    beskrivendeId = "søknadstidspunkt",
                    type = Tekst,
                    svar = søknadstidspunkt.toString(),
                    ident = ident,
                    søknadId = quizSøknadId,
                )
            opplysningRepository.lagre(opplysning)
            opplysningRepository.lagre(søknadstidpsunktOpplysning)

            behovløser.løs(lagBehovmeldingUtenSøknadId(ident))

            testRapid.inspektør.message(0)["@løsning"]["Søknadsdata"].also { løsning ->
                val verdi = løsning["verdi"]
                verdi["avtjentVerneplikt"].asBoolean() shouldBe expectedAvtjentVerneplikt
                verdi["søknadId"].asUUID() shouldBe quizSøknadId
                verdi["ønskerDagpengerFraDato"].asLocalDate() shouldBe now
                løsning["gjelderFra"].asLocalDate() shouldBe søknadstidspunkt.toLocalDate()
            }
            testRapid.reset()
        }
    }

    @Test
    fun `Andre ytelser for søkeren fra orkestrator søknad`() {
        val cases =
            listOf(
                Triple("ja", "nei", true),
                Triple("nei", "ja", true),
                Triple("nei", "nei", false),
                Triple("ja", "ja", true),
            )
        cases.forEach {
            (
                mottarDuPengestøtteFraAndreEnnNav,
                mottarDuAndreUtbetalingerEllerØkonomiskeGoderFraTidligereArbeidsgiver,
                forventetHarAndreYtelser,
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
                    mottarDuPengestøtteFraAndreEnnNav,
                    mottarDuAndreUtbetalingerEllerØkonomiskeGoderFraTidligereArbeidsgiver,
                )
            val søknadstidspunkt = ZonedDateTime.now()
            every {
                søknadRepository.hent(any())
            } returns
                Søknad(
                    søknadId = søknadId,
                    ident = ident,
                    tilstand = Tilstand.INNSENDT,
                    innsendtTidspunkt = søknadstidspunkt.toLocalDateTime(),
                )

            behovløser.løs(lagBehovmeldingUtenSøknadId(ident))
            testRapid.inspektør.message(0)["@løsning"]["Søknadsdata"].also { løsning ->
                val verdi = løsning["verdi"]
                verdi["harAndreYtelser"].asBoolean() shouldBe forventetHarAndreYtelser
                verdi["søknadId"].asUUID() shouldBe søknadId
                verdi["ønskerDagpengerFraDato"].asLocalDate() shouldBe now
                løsning["gjelderFra"].asLocalDate() shouldBe søknadstidspunkt.toLocalDate()
            }
            testRapid.reset()
        }
    }

    @Test
    fun `Andre ytelser for søkeren fra quiz søknad`() {
        val cases =
            listOf(
                Triple(true, false, true),
                Triple(false, true, true),
                Triple(false, false, false),
                Triple(true, true, true),
            )
        cases.forEach {
            (
                mottarDuPengestøtteFraAndreEnnNav,
                mottarDuAndreUtbetalingerEllerØkonomiskeGoderFraTidligereArbeidsgiver,
                forventetHarAndreYtelser,
            ),
            ->
            val quizSøknadId = UUID.randomUUID()

            every {
                søknadRepository.hentSøknadIdFraJournalPostId(any(), any())
            } returns quizSøknadId

            val tidligereArbeidsgiverYtelseFraQuiz =
                QuizOpplysning(
                    beskrivendeId = "faktum.utbetaling-eller-okonomisk-gode-tidligere-arbeidsgiver",
                    type = Boolsk,
                    svar = mottarDuAndreUtbetalingerEllerØkonomiskeGoderFraTidligereArbeidsgiver,
                    ident = ident,
                    søknadId = quizSøknadId,
                )

            val andreYtelserFraQuiz =
                QuizOpplysning(
                    beskrivendeId = "faktum.andre-ytelser-mottatt-eller-sokt",
                    type = Boolsk,
                    svar = mottarDuPengestøtteFraAndreEnnNav,
                    ident = ident,
                    søknadId = quizSøknadId,
                )
            val søknadstidspunkt = ZonedDateTime.now()
            val søknadstidpsunktOpplysning =
                QuizOpplysning(
                    beskrivendeId = "søknadstidspunkt",
                    type = Tekst,
                    svar = søknadstidspunkt.toString(),
                    ident = ident,
                    søknadId = quizSøknadId,
                )
            opplysningRepository.lagre(tidligereArbeidsgiverYtelseFraQuiz)
            opplysningRepository.lagre(andreYtelserFraQuiz)
            opplysningRepository.lagre(søknadstidpsunktOpplysning)

            behovløser.løs(lagBehovmeldingUtenSøknadId(ident))

            behovløser.løs(lagBehovmeldingUtenSøknadId(ident))
            testRapid.inspektør.message(0)["@løsning"]["Søknadsdata"].also { løsning ->
                val verdi = løsning["verdi"]
                verdi["harAndreYtelser"].asBoolean() shouldBe forventetHarAndreYtelser
                verdi["søknadId"].asUUID() shouldBe quizSøknadId
                verdi["ønskerDagpengerFraDato"].asLocalDate() shouldBe now
                løsning["gjelderFra"].asLocalDate() shouldBe søknadstidspunkt.toLocalDate()
            }
            testRapid.reset()
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

        val søknadstidspunkt = ZonedDateTime.now()
        every {
            søknadRepository.hent(any())
        } returns
            Søknad(
                søknadId = søknadId,
                ident = ident,
                tilstand = Tilstand.INNSENDT,
                innsendtTidspunkt = søknadstidspunkt.toLocalDateTime(),
            )

        behovløser.løs(lagBehovmeldingUtenSøknadId(ident))
        testRapid.inspektør.message(0)["@løsning"]["Søknadsdata"].also { løsning ->
            val verdi = løsning["verdi"]
            val avsluttetArbeidsforhold = verdi["avsluttetArbeidsforhold"]
            avsluttetArbeidsforhold.size() shouldBe 2
            avsluttetArbeidsforhold[0]["sluttårsak"].asText() shouldBe "SAGT_OPP_AV_ARBEIDSGIVER"
            avsluttetArbeidsforhold[0]["fiskeforedling"].asBoolean() shouldBe false
            avsluttetArbeidsforhold[0]["land"].asText() shouldBe "NOR"
            avsluttetArbeidsforhold[1]["sluttårsak"].asText() shouldBe "PERMITTERT"
            avsluttetArbeidsforhold[1]["fiskeforedling"].asBoolean() shouldBe true
            avsluttetArbeidsforhold[1]["land"].asText() shouldBe "SWE"
            verdi["søknadId"].asUUID() shouldBe søknadId
            verdi["ønskerDagpengerFraDato"].asLocalDate() shouldBe now
            løsning["gjelderFra"].asLocalDate() shouldBe søknadstidspunkt.toLocalDate()
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

        val søknadstidspunkt = ZonedDateTime.now()
        every {
            søknadRepository.hent(any())
        } returns
            Søknad(
                søknadId = søknadId,
                ident = ident,
                tilstand = Tilstand.INNSENDT,
                innsendtTidspunkt = søknadstidspunkt.toLocalDateTime(),
            )

        behovløser.løs(lagBehovmeldingUtenSøknadId(ident))
        testRapid.inspektør.message(0)["@løsning"]["Søknadsdata"].also { løsning ->
            val verdi = løsning["verdi"]
            val avsluttetArbeidsforhold = verdi["avsluttetArbeidsforhold"]
            avsluttetArbeidsforhold.size() shouldBe 2
            avsluttetArbeidsforhold[0]["sluttårsak"].asText() shouldBe "SAGT_OPP_AV_ARBEIDSGIVER"
            avsluttetArbeidsforhold[0]["fiskeforedling"].asBoolean() shouldBe false
            avsluttetArbeidsforhold[0]["land"].asText() shouldBe "NOR"
            avsluttetArbeidsforhold[1]["sluttårsak"].asText() shouldBe "PERMITTERT"
            avsluttetArbeidsforhold[1]["fiskeforedling"].asBoolean() shouldBe true
            avsluttetArbeidsforhold[1]["land"].asText() shouldBe "SWE"
            verdi["søknadId"].asUUID() shouldBe søknadId
            verdi["ønskerDagpengerFraDato"].asLocalDate() shouldBe now
            løsning["gjelderFra"].asLocalDate() shouldBe søknadstidspunkt.toLocalDate()
        }
    }

    @Test
    fun `Søker med arbeidsforhold returneres med liste over sluttårsak, permittert fra fiskeforedling og land de jobbet i quiz søknad`() {
        val arbeidsforhold =
            listOf(
                ArbeidsforholdSvar(
                    navn = "Kiwi",
                    land = "NOR",
                    sluttårsak = Sluttårsak.SAGT_OPP_AV_ARBEIDSGIVER,
                ),
                ArbeidsforholdSvar(
                    navn = "Meny",
                    land = "SWE",
                    sluttårsak = Sluttårsak.PERMITTERT_FISKEFOREDLING,
                ),
            )
        val opplysning =
            QuizOpplysning(
                beskrivendeId = "faktum.arbeidsforhold",
                type = Arbeidsforhold,
                svar = arbeidsforhold,
                ident = ident,
                søknadId = søknadId,
            )
        val søknadstidspunkt = ZonedDateTime.now()
        val søknadstidpsunktOpplysning =
            QuizOpplysning(
                beskrivendeId = "søknadstidspunkt",
                type = Tekst,
                svar = søknadstidspunkt.toString(),
                ident = ident,
                søknadId = søknadId,
            )
        opplysningRepository.lagre(opplysning)
        opplysningRepository.lagre(søknadstidpsunktOpplysning)

        behovløser.løs(lagBehovmeldingUtenSøknadId(ident))
        testRapid.inspektør.message(0)["@løsning"]["Søknadsdata"].also { løsning ->
            val verdi = løsning["verdi"]
            val avsluttetArbeidsforhold = verdi["avsluttetArbeidsforhold"]
            avsluttetArbeidsforhold.size() shouldBe 2
            avsluttetArbeidsforhold[0]["sluttårsak"].asText() shouldBe "SAGT_OPP_AV_ARBEIDSGIVER"
            avsluttetArbeidsforhold[0]["fiskeforedling"].asBoolean() shouldBe false
            avsluttetArbeidsforhold[0]["land"].asText() shouldBe "NOR"
            avsluttetArbeidsforhold[1]["sluttårsak"].asText() shouldBe "PERMITTERT"
            avsluttetArbeidsforhold[1]["fiskeforedling"].asBoolean() shouldBe true
            avsluttetArbeidsforhold[1]["land"].asText() shouldBe "SWE"
            verdi["søknadId"].asUUID() shouldBe søknadId
            verdi["ønskerDagpengerFraDato"].asLocalDate() shouldBe now
            løsning["gjelderFra"].asLocalDate() shouldBe søknadstidspunkt.toLocalDate()
        }
    }

    @Test
    fun `Søker uten arbeidsforhold returneres med tom list for quiz søknad`() {
        behovløser.løs(lagBehovmeldingUtenSøknadId(ident))
        testRapid.inspektør.message(0)["@løsning"]["Søknadsdata"].also { løsning ->
            val verdi = løsning["verdi"]
            verdi["avsluttetArbeidsforhold"].size() shouldBe 0
            verdi["søknadId"].asUUID() shouldBe søknadId
            verdi["ønskerDagpengerFraDato"].asLocalDate() shouldBe now
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

        val søknadstidspunkt = ZonedDateTime.now()
        every {
            søknadRepository.hent(any())
        } returns
            Søknad(
                søknadId = søknadId,
                ident = ident,
                tilstand = Tilstand.INNSENDT,
                innsendtTidspunkt = søknadstidspunkt.toLocalDateTime(),
            )

        behovløser.løs(lagBehovmeldingUtenSøknadId(ident))
        testRapid.inspektør.message(0)["@løsning"]["Søknadsdata"].also { løsning ->
            val verdi = løsning["verdi"]
            verdi["avsluttetArbeidsforhold"].size() shouldBe 0
            verdi["søknadId"].asUUID() shouldBe søknadId
            verdi["ønskerDagpengerFraDato"].asLocalDate() shouldBe now
            løsning["gjelderFra"].asLocalDate() shouldBe søknadstidspunkt.toLocalDate()
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

        val søknadstidspunkt = ZonedDateTime.now()
        every {
            søknadRepository.hent(any())
        } returns
            Søknad(
                søknadId = søknadId,
                ident = ident,
                tilstand = Tilstand.INNSENDT,
                innsendtTidspunkt = søknadstidspunkt.toLocalDateTime(),
            )

        behovløser.løs(lagBehovmeldingUtenSøknadId(ident))
        testRapid.inspektør.message(0)["@løsning"]["Søknadsdata"].also { løsning ->
            val verdi = løsning["verdi"]
            verdi["harBarn"].asBoolean() shouldBe true
            verdi["søknadId"].asUUID() shouldBe søknadId
            verdi["ønskerDagpengerFraDato"].asLocalDate() shouldBe now
            løsning["gjelderFra"].asLocalDate() shouldBe søknadstidspunkt.toLocalDate()
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

        val søknadstidspunkt = ZonedDateTime.now()
        every {
            søknadRepository.hent(any())
        } returns
            Søknad(
                søknadId = søknadId,
                ident = ident,
                tilstand = Tilstand.INNSENDT,
                innsendtTidspunkt = søknadstidspunkt.toLocalDateTime(),
            )

        behovløser.løs(lagBehovmeldingUtenSøknadId(ident))
        testRapid.inspektør.message(0)["@løsning"]["Søknadsdata"].also { løsning ->
            val verdi = løsning["verdi"]
            verdi["harBarn"].asBoolean() shouldBe true
            verdi["søknadId"].asUUID() shouldBe søknadId
            verdi["ønskerDagpengerFraDato"].asLocalDate() shouldBe now
            løsning["gjelderFra"].asLocalDate() shouldBe søknadstidspunkt.toLocalDate()
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

        val søknadstidspunkt = ZonedDateTime.now()
        every {
            søknadRepository.hent(any())
        } returns
            Søknad(
                søknadId = søknadId,
                ident = ident,
                tilstand = Tilstand.INNSENDT,
                innsendtTidspunkt = søknadstidspunkt.toLocalDateTime(),
            )

        behovløser.løs(lagBehovmeldingUtenSøknadId(ident))
        testRapid.inspektør.message(0)["@løsning"]["Søknadsdata"].also { løsning ->
            val verdi = løsning["verdi"]
            verdi["harBarn"].asBoolean() shouldBe true
            verdi["søknadId"].asUUID() shouldBe søknadId
            verdi["ønskerDagpengerFraDato"].asLocalDate() shouldBe now
            løsning["gjelderFra"].asLocalDate() shouldBe søknadstidspunkt.toLocalDate()
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
        val søknadstidspunkt = ZonedDateTime.now()
        every {
            søknadRepository.hent(any())
        } returns
            Søknad(
                søknadId = søknadId,
                ident = ident,
                tilstand = Tilstand.INNSENDT,
                innsendtTidspunkt = søknadstidspunkt.toLocalDateTime(),
            )

        behovløser.løs(lagBehovmeldingUtenSøknadId(ident))
        testRapid.inspektør.message(0)["@løsning"]["Søknadsdata"].also { løsning ->
            val verdi = løsning["verdi"]
            verdi["harBarn"].asBoolean() shouldBe false
            verdi["søknadId"].asUUID() shouldBe søknadId
            verdi["ønskerDagpengerFraDato"].asLocalDate() shouldBe now
            løsning["gjelderFra"].asLocalDate() shouldBe søknadstidspunkt.toLocalDate()
        }
    }

    @Test
    fun `Søker med pdl og egne barn harBarn verdien satt til true fra quiz søknad`() {
        val egneBarnOpplysning =
            QuizOpplysning(
                beskrivendeId = "faktum.barn-liste",
                type = Barn,
                svar = manuelLagteBarnFraQuiz(),
                ident = ident,
                søknadId = søknadId,
            )
        val pdlBarnOpplysning =
            QuizOpplysning(
                beskrivendeId = "faktum.register.barn-liste",
                type = Barn,
                svar = pdlBarnFraQuiz(),
                ident = ident,
                søknadId = søknadId,
            )
        val søknadstidspunkt = ZonedDateTime.now()
        val søknadstidpsunktOpplysning =
            QuizOpplysning(
                beskrivendeId = "søknadstidspunkt",
                type = Tekst,
                svar = søknadstidspunkt.toString(),
                ident = ident,
                søknadId = søknadId,
            )
        opplysningRepository.lagre(egneBarnOpplysning)
        opplysningRepository.lagre(pdlBarnOpplysning)
        opplysningRepository.lagre(søknadstidpsunktOpplysning)

        behovløser.løs(lagBehovmeldingUtenSøknadId(ident))
        testRapid.inspektør.message(0)["@løsning"]["Søknadsdata"].also { løsning ->
            val verdi = løsning["verdi"]
            verdi["harBarn"].asBoolean() shouldBe true
            verdi["søknadId"].asUUID() shouldBe søknadId
            verdi["ønskerDagpengerFraDato"].asLocalDate() shouldBe now
            løsning["gjelderFra"].asLocalDate() shouldBe søknadstidspunkt.toLocalDate()
        }
    }

    @Test
    fun `Søker med kun pdl harBarn verdien satt til true fra quiz søknad`() {
        val pdlBarnOpplysning =
            QuizOpplysning(
                beskrivendeId = "faktum.register.barn-liste",
                type = Barn,
                svar = pdlBarnFraQuiz(),
                ident = ident,
                søknadId = søknadId,
            )
        val søknadstidspunkt = ZonedDateTime.now()
        val søknadstidpsunktOpplysning =
            QuizOpplysning(
                beskrivendeId = "søknadstidspunkt",
                type = Tekst,
                svar = søknadstidspunkt.toString(),
                ident = ident,
                søknadId = søknadId,
            )
        opplysningRepository.lagre(pdlBarnOpplysning)
        opplysningRepository.lagre(søknadstidpsunktOpplysning)

        behovløser.løs(lagBehovmeldingUtenSøknadId(ident))
        testRapid.inspektør.message(0)["@løsning"]["Søknadsdata"].also { løsning ->
            val verdi = løsning["verdi"]
            verdi["harBarn"].asBoolean() shouldBe true
            verdi["søknadId"].asUUID() shouldBe søknadId
            verdi["ønskerDagpengerFraDato"].asLocalDate() shouldBe now
            løsning["gjelderFra"].asLocalDate() shouldBe søknadstidspunkt.toLocalDate()
        }
    }

    @Test
    fun `Søker med kun egne barn harBarn verdien satt til true fra quiz søknad`() {
        val egneBarnOpplysning =
            QuizOpplysning(
                beskrivendeId = "faktum.barn-liste",
                type = Barn,
                svar = manuelLagteBarnFraQuiz(),
                ident = ident,
                søknadId = søknadId,
            )
        val søknadstidspunkt = ZonedDateTime.now()
        val søknadstidpsunktOpplysning =
            QuizOpplysning(
                beskrivendeId = "søknadstidspunkt",
                type = Tekst,
                svar = søknadstidspunkt.toString(),
                ident = ident,
                søknadId = søknadId,
            )
        opplysningRepository.lagre(egneBarnOpplysning)
        opplysningRepository.lagre(søknadstidpsunktOpplysning)

        behovløser.løs(lagBehovmeldingUtenSøknadId(ident))
        testRapid.inspektør.message(0)["@løsning"]["Søknadsdata"].also { løsning ->
            val verdi = løsning["verdi"]
            verdi["harBarn"].asBoolean() shouldBe true
            verdi["søknadId"].asUUID() shouldBe søknadId
            verdi["ønskerDagpengerFraDato"].asLocalDate() shouldBe now
            løsning["gjelderFra"].asLocalDate() shouldBe søknadstidspunkt.toLocalDate()
        }
    }

    @Test
    fun `Søker med ingen barn får harBarn verdien satt til false fra quiz søknad`() {
        val søknadstidspunkt = ZonedDateTime.now()
        every {
            søknadRepository.hent(any())
        } returns
            Søknad(
                søknadId = søknadId,
                ident = ident,
                tilstand = Tilstand.INNSENDT,
                innsendtTidspunkt = søknadstidspunkt.toLocalDateTime(),
            )

        behovløser.løs(lagBehovmeldingUtenSøknadId(ident))

        testRapid.inspektør.message(0)["@løsning"]["Søknadsdata"].also { løsning ->
            val verdi = løsning["verdi"]
            verdi["harBarn"].asBoolean() shouldBe false
            verdi["søknadId"].asUUID() shouldBe søknadId
            verdi["ønskerDagpengerFraDato"].asLocalDate() shouldBe now
            løsning["gjelderFra"].asLocalDate() shouldBe søknadstidspunkt.toLocalDate()
        }
    }

    @Test
    fun `Teste reell-arbeidssøker for orkestrator søknad hvor noen verdier er true`() {
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

        val søknadstidspunkt = ZonedDateTime.now()
        every {
            søknadRepository.hent(any())
        } returns
            Søknad(
                søknadId = søknadId,
                ident = ident,
                tilstand = Tilstand.INNSENDT,
                innsendtTidspunkt = søknadstidspunkt.toLocalDateTime(),
            )

        behovløser.løs(lagBehovmeldingUtenSøknadId(ident))
        testRapid.inspektør.message(0)["@løsning"]["Søknadsdata"].also { løsning ->
            val verdi = løsning["verdi"]
            val reellArbeidssøker = verdi["reellArbeidssøker"]
            reellArbeidssøker["helse"].asBoolean() shouldBe true
            reellArbeidssøker["geografi"].asBoolean() shouldBe false
            reellArbeidssøker["deltid"].asBoolean() shouldBe true
            reellArbeidssøker["yrke"].asBoolean() shouldBe false
            verdi["søknadId"].asUUID() shouldBe søknadId
            verdi["ønskerDagpengerFraDato"].asLocalDate() shouldBe now
            løsning["gjelderFra"].asLocalDate() shouldBe søknadstidspunkt.toLocalDate()
        }
    }

    @Test
    fun `Teste reell-arbeidssøker for orkestrator søknad hvor alle verdier er true`() {
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

        val søknadstidspunkt = ZonedDateTime.now()
        every {
            søknadRepository.hent(any())
        } returns
            Søknad(
                søknadId = søknadId,
                ident = ident,
                tilstand = Tilstand.INNSENDT,
                innsendtTidspunkt = søknadstidspunkt.toLocalDateTime(),
            )

        behovløser.løs(lagBehovmeldingUtenSøknadId(ident))
        testRapid.inspektør.message(0)["@løsning"]["Søknadsdata"].also { løsning ->
            val verdi = løsning["verdi"]
            val reellArbeidssøker = verdi["reellArbeidssøker"]
            reellArbeidssøker["helse"].asBoolean() shouldBe true
            reellArbeidssøker["geografi"].asBoolean() shouldBe true
            reellArbeidssøker["deltid"].asBoolean() shouldBe true
            reellArbeidssøker["yrke"].asBoolean() shouldBe true
            verdi["søknadId"].asUUID() shouldBe søknadId
            verdi["ønskerDagpengerFraDato"].asLocalDate() shouldBe now
            løsning["gjelderFra"].asLocalDate() shouldBe søknadstidspunkt.toLocalDate()
        }
    }

    @Test
    fun `Teste reell-arbeidssøker for orkestrator søknad hvor ingen verdier er true`() {
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

        val søknadstidspunkt = ZonedDateTime.now()
        every {
            søknadRepository.hent(any())
        } returns
            Søknad(
                søknadId = søknadId,
                ident = ident,
                tilstand = Tilstand.INNSENDT,
                innsendtTidspunkt = søknadstidspunkt.toLocalDateTime(),
            )

        behovløser.løs(lagBehovmeldingUtenSøknadId(ident))
        testRapid.inspektør.message(0)["@løsning"]["Søknadsdata"].also { løsning ->
            val verdi = løsning["verdi"]
            val reellArbeidssøker = verdi["reellArbeidssøker"]
            reellArbeidssøker["helse"].asBoolean() shouldBe false
            reellArbeidssøker["geografi"].asBoolean() shouldBe false
            reellArbeidssøker["deltid"].asBoolean() shouldBe false
            reellArbeidssøker["yrke"].asBoolean() shouldBe false
            verdi["søknadId"].asUUID() shouldBe søknadId
            verdi["ønskerDagpengerFraDato"].asLocalDate() shouldBe now
            løsning["gjelderFra"].asLocalDate() shouldBe søknadstidspunkt.toLocalDate()
        }
    }

    @Test
    fun `Teste reell-arbeidssøker for quiz søknad hvor alle verdier skal være true`() {
        val alleTypeArbeidOpplysning = hentAlleTypeArbeidForReellArbeidssøkerFraQuiz(ident, søknadId, true)
        val kanDuJobbeIHeleNorge =
            hentKanDuJobbeIHeleNorgeForReellArbeidssøkerFraQuiz(
                ident,
                søknadId,
                true,
            )
        val kanDuJobbeBådeHeltidOgDeltid = kanDuJobbeBådeHeltidOgDeltidForReellArbeidssøkerFraQuiz(ident, søknadId, true)
        val erDuVilligTilÅBytteYrkeEllerGåNedILønn =
            erDuVilligTilÅBytteYrkeEllerGåNedILønnForReellArbeidssøkerFraQuiz(
                ident,
                søknadId,
                true,
            )

        // Må også lagre søknadstidspunkt fordi det er denne som brukes for å sette gjelderFra i første omgang
        val søknadstidspunkt = ZonedDateTime.now()
        val søknadstidpsunktOpplysning =
            QuizOpplysning(
                beskrivendeId = "søknadstidspunkt",
                type = Tekst,
                svar = søknadstidspunkt.toString(),
                ident = ident,
                søknadId = søknadId,
            )

        opplysningRepository.lagre(alleTypeArbeidOpplysning)
        opplysningRepository.lagre(kanDuJobbeIHeleNorge)
        opplysningRepository.lagre(kanDuJobbeBådeHeltidOgDeltid)
        opplysningRepository.lagre(erDuVilligTilÅBytteYrkeEllerGåNedILønn)
        opplysningRepository.lagre(søknadstidpsunktOpplysning)

        behovløser.løs(lagBehovmeldingUtenSøknadId(ident))
        testRapid.inspektør.message(0)["@løsning"]["Søknadsdata"].also { løsning ->
            val verdi = løsning["verdi"]
            val reellArbeidssøker = verdi["reellArbeidssøker"]
            reellArbeidssøker["helse"].asBoolean() shouldBe true
            reellArbeidssøker["geografi"].asBoolean() shouldBe true
            reellArbeidssøker["deltid"].asBoolean() shouldBe true
            reellArbeidssøker["yrke"].asBoolean() shouldBe true
            verdi["søknadId"].asUUID() shouldBe søknadId
            verdi["ønskerDagpengerFraDato"].asLocalDate() shouldBe now
            løsning["gjelderFra"].asLocalDate() shouldBe søknadstidspunkt.toLocalDate()
        }
    }

    @Test
    fun `Teste reell-arbeidssøker for quiz søknad hvor noen verdier er true`() {
        val alleTypeArbeidOpplysning = hentAlleTypeArbeidForReellArbeidssøkerFraQuiz(ident, søknadId, true)
        val kanDuJobbeIHeleNorge =
            hentKanDuJobbeIHeleNorgeForReellArbeidssøkerFraQuiz(
                ident,
                søknadId,
                false,
            )
        val kanDuJobbeBådeHeltidOgDeltid = kanDuJobbeBådeHeltidOgDeltidForReellArbeidssøkerFraQuiz(ident, søknadId, true)
        val erDuVilligTilÅBytteYrkeEllerGåNedILønn =
            erDuVilligTilÅBytteYrkeEllerGåNedILønnForReellArbeidssøkerFraQuiz(
                ident,
                søknadId,
                false,
            )

        // Må også lagre søknadstidspunkt fordi det er denne som brukes for å sette gjelderFra i første omgang
        val søknadstidspunkt = ZonedDateTime.now()
        val søknadstidpsunktOpplysning =
            QuizOpplysning(
                beskrivendeId = "søknadstidspunkt",
                type = Tekst,
                svar = søknadstidspunkt.toString(),
                ident = ident,
                søknadId = søknadId,
            )

        opplysningRepository.lagre(alleTypeArbeidOpplysning)
        opplysningRepository.lagre(kanDuJobbeIHeleNorge)
        opplysningRepository.lagre(kanDuJobbeBådeHeltidOgDeltid)
        opplysningRepository.lagre(erDuVilligTilÅBytteYrkeEllerGåNedILønn)
        opplysningRepository.lagre(søknadstidpsunktOpplysning)

        behovløser.løs(lagBehovmeldingUtenSøknadId(ident))
        testRapid.inspektør.message(0)["@løsning"]["Søknadsdata"].also { løsning ->
            val verdi = løsning["verdi"]
            val reellArbeidssøker = verdi["reellArbeidssøker"]
            reellArbeidssøker["helse"].asBoolean() shouldBe true
            reellArbeidssøker["geografi"].asBoolean() shouldBe false
            reellArbeidssøker["deltid"].asBoolean() shouldBe true
            reellArbeidssøker["yrke"].asBoolean() shouldBe false
            verdi["søknadId"].asUUID() shouldBe søknadId
            verdi["ønskerDagpengerFraDato"].asLocalDate() shouldBe now
            løsning["gjelderFra"].asLocalDate() shouldBe søknadstidspunkt.toLocalDate()
        }
    }

    @Test
    fun `Søker som gjenopptar dagpenger fra quiz søknad`() {
        val søknadstidspunkt = ZonedDateTime.now()

        val gjenopptarDagpengerOpplysning =
            QuizOpplysning(
                beskrivendeId = "faktum.arbeidsforhold.gjenopptak.soknadsdato-gjenopptak",
                type = Dato,
                svar = søknadstidspunkt.toLocalDate(),
                ident = ident,
                søknadId = søknadId,
            )
        val søknadstidpsunktOpplysning =
            QuizOpplysning(
                beskrivendeId = "søknadstidspunkt",
                type = Tekst,
                svar = søknadstidspunkt.toString(),
                ident = ident,
                søknadId = søknadId,
            )
        opplysningRepository.lagre(gjenopptarDagpengerOpplysning)
        opplysningRepository.lagre(søknadstidpsunktOpplysning)

        behovløser.løs(lagBehovmeldingUtenSøknadId(ident))
        testRapid.inspektør.message(0)["@løsning"]["Søknadsdata"].also { løsning ->
            val verdi = løsning["verdi"]
            verdi["ønskerDagpengerFraDato"].asLocalDate() shouldBe søknadstidspunkt.toLocalDate()
            verdi["søknadId"].asUUID() shouldBe søknadId
            løsning["gjelderFra"].asLocalDate() shouldBe søknadstidspunkt.toLocalDate()
        }
    }

    @Test
    fun `Søker som dagpenger fra dato fra quiz søknad`() {
        val søknadstidspunkt = ZonedDateTime.now()

        val dagpengerFraDatoOpplysning =
            QuizOpplysning(
                beskrivendeId = "faktum.dagpenger-soknadsdato",
                type = Dato,
                svar = søknadstidspunkt.toLocalDate(),
                ident = ident,
                søknadId = søknadId,
            )
        val søknadstidpsunktOpplysning =
            QuizOpplysning(
                beskrivendeId = "søknadstidspunkt",
                type = Tekst,
                svar = søknadstidspunkt.toString(),
                ident = ident,
                søknadId = søknadId,
            )
        opplysningRepository.lagre(dagpengerFraDatoOpplysning)
        opplysningRepository.lagre(søknadstidpsunktOpplysning)

        behovløser.løs(lagBehovmeldingUtenSøknadId(ident))
        testRapid.inspektør.message(0)["@løsning"]["Søknadsdata"].also { løsning ->
            val verdi = løsning["verdi"]
            verdi["ønskerDagpengerFraDato"].asLocalDate() shouldBe søknadstidspunkt.toLocalDate()
            verdi["søknadId"].asUUID() shouldBe søknadId
            løsning["gjelderFra"].asLocalDate() shouldBe søknadstidspunkt.toLocalDate()
        }
    }

    @Test
    fun `Søker som dagpenger fra dato fra orkestrator søknad`() {
        val søknadstidspunkt = ZonedDateTime.now()
        every {
            seksjonRepository.hentSeksjonsvarEllerKastException(
                ident,
                søknadId,
                "din-situasjon",
            )
        } returns
            dinSituasjonMedDagpengerFraDato(søknadstidspunkt.toLocalDate())

        every {
            søknadRepository.hent(any())
        } returns
            Søknad(
                søknadId = søknadId,
                ident = ident,
                tilstand = Tilstand.INNSENDT,
                innsendtTidspunkt = søknadstidspunkt.toLocalDateTime(),
            )

        behovløser.løs(lagBehovmeldingUtenSøknadId(ident))
        testRapid.inspektør.message(0)["@løsning"]["Søknadsdata"].also { løsning ->
            val verdi = løsning["verdi"]
            verdi["ønskerDagpengerFraDato"].asLocalDate() shouldBe søknadstidspunkt.toLocalDate()
            verdi["søknadId"].asUUID() shouldBe søknadId
            løsning["gjelderFra"].asLocalDate() shouldBe søknadstidspunkt.toLocalDate()
        }
    }

    @Test
    fun `Søker som gjenopptar dagpenger fra orkestrator søknad`() {
        val søknadstidspunkt = ZonedDateTime.now()
        every {
            seksjonRepository.hentSeksjonsvarEllerKastException(
                ident,
                søknadId,
                "din-situasjon",
            )
        } returns
            dinSituasjonMedGjenopptakelseOrkestratorJson(søknadstidspunkt.toLocalDate())
        every {
            søknadRepository.hent(any())
        } returns
            Søknad(
                søknadId = søknadId,
                ident = ident,
                tilstand = Tilstand.INNSENDT,
                innsendtTidspunkt = søknadstidspunkt.toLocalDateTime(),
            )

        behovløser.løs(lagBehovmeldingUtenSøknadId(ident))
        testRapid.inspektør.message(0)["@løsning"]["Søknadsdata"].also { løsning ->
            val verdi = løsning["verdi"]
            verdi["ønskerDagpengerFraDato"].asLocalDate() shouldBe søknadstidspunkt.toLocalDate()
            verdi["søknadId"].asUUID() shouldBe søknadId
            løsning["gjelderFra"].asLocalDate() shouldBe søknadstidspunkt.toLocalDate()
        }
    }
}
