package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.BarnetilleggV2
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.BarnetilleggBehovLøser.Companion.BESKRIVENDE_ID_EGNE_BARN
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.BarnetilleggBehovLøser.Companion.BESKRIVENDE_ID_PDL_BARN
import no.nav.dagpenger.soknad.orkestrator.opplysning.SaksbehandlerBarnRepository
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.QuizOpplysning
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Barn
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.BarnSvar
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Tekst
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonRepository
import no.nav.dagpenger.soknad.orkestrator.utils.InMemoryQuizOpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.utils.asUUID
import no.nav.dagpenger.soknad.orkestrator.utils.januar
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.util.UUID
import java.util.UUID.randomUUID

class BarnetilleggV2BehovLøserTest {
    val opplysningRepository = InMemoryQuizOpplysningRepository()
    val quizOpplysningRepositorySpy = spyk<QuizOpplysningRepository>(opplysningRepository)
    val søknadRepository = mockk<SøknadRepository>(relaxed = true)
    val seksjonRepository = mockk<SeksjonRepository>(relaxed = true)
    val saksbehandlerBarnRepository =
        mockk<SaksbehandlerBarnRepository>(relaxed = true).also {
            every { it.hentBarn(any()) } returns null
        }
    val testRapid = TestRapid()
    val behovløser =
        BarnetilleggV2BehovLøser(testRapid, quizOpplysningRepositorySpy, søknadRepository, seksjonRepository, saksbehandlerBarnRepository)
    val ident = "12345678910"
    val søknadId: UUID = randomUUID()

    @Test
    fun `løser behov om barn som forventet hvis søknaden har barn`() {
        val pdlBarn =
            QuizOpplysning(
                beskrivendeId = BESKRIVENDE_ID_PDL_BARN,
                type = Barn,
                ident = ident,
                søknadId = søknadId,
                svar =
                    listOf(
                        BarnSvar(
                            barnSvarId = randomUUID(),
                            fornavnOgMellomnavn = "Ola",
                            etternavn = "Nordmann",
                            fødselsdato = 1.januar(2000),
                            statsborgerskap = "NOR",
                            forsørgerBarnet = true,
                            fraRegister = true,
                            kvalifisererTilBarnetillegg = true,
                            barnetilleggFom = 1.januar(2000),
                            barnetilleggTom = 1.januar(2018),
                            endretAv = "123",
                            begrunnelse = "Begrunnelse for endring",
                        ),
                        BarnSvar(
                            barnSvarId = randomUUID(),
                            fornavnOgMellomnavn = "Per",
                            etternavn = "Nordmann",
                            fødselsdato = 1.januar(2000),
                            statsborgerskap = "NOR",
                            forsørgerBarnet = false,
                            fraRegister = true,
                            kvalifisererTilBarnetillegg = false,
                        ),
                    ),
            )
        val egetBarn =
            QuizOpplysning(
                beskrivendeId = BESKRIVENDE_ID_EGNE_BARN,
                type = Barn,
                ident = ident,
                søknadId = søknadId,
                svar =
                    listOf(
                        BarnSvar(
                            barnSvarId = randomUUID(),
                            fornavnOgMellomnavn = "Per",
                            etternavn = "Utland",
                            fødselsdato = 1.januar(2000),
                            statsborgerskap = "UTL",
                            forsørgerBarnet = true,
                            fraRegister = false,
                            kvalifisererTilBarnetillegg = true,
                        ),
                    ),
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

        opplysningRepository.lagre(pdlBarn)
        opplysningRepository.lagre(egetBarn)
        opplysningRepository.lagre(søknadstidpsunktOpplysning)
        val lagretSøknadbarnId = opplysningRepository.lagreBarnSøknadMapping(søknadId = søknadId)

        behovløser.løs(lagBehovmelding(ident, søknadId, BarnetilleggV2))

        val barnetilleggV2Løsning = testRapid.inspektør.field(0, "@løsning")[BarnetilleggV2.name]["verdi"]
        val løsteBarn = barnetilleggV2Løsning["barn"]
        verify(exactly = 1) { quizOpplysningRepositorySpy.hentEllerOpprettSøknadbarnId(any()) }
        barnetilleggV2Løsning["søknadbarnId"].asUUID() shouldBe lagretSøknadbarnId
        løsteBarn.size() shouldBe 3
        løsteBarn[0].also {
            it["fornavnOgMellomnavn"].asString() shouldBe "Ola"
            it["etternavn"].asString() shouldBe "Nordmann"
            it["fødselsdato"].asString() shouldBe "2000-01-01"
            it["statsborgerskap"].asString() shouldBe "NOR"
            it["kvalifiserer"].asBoolean() shouldBe true
            it["barnetilleggFom"].asLocalDate() shouldBe 1.januar(2000)
            it["barnetilleggTom"].asLocalDate() shouldBe 1.januar(2018)
            it["endretAv"].asString() shouldBe "123"
            it["begrunnelse"].asString() shouldBe "Begrunnelse for endring"
        }
        løsteBarn[1].also {
            it["fornavnOgMellomnavn"].asString() shouldBe "Per"
            it["etternavn"].asString() shouldBe "Nordmann"
            it["fødselsdato"].asString() shouldBe "2000-01-01"
            it["statsborgerskap"].asString() shouldBe "NOR"
            it["kvalifiserer"].asBoolean() shouldBe false
        }
        løsteBarn[2].also {
            it["fornavnOgMellomnavn"].asString() shouldBe "Per"
            it["etternavn"].asString() shouldBe "Utland"
            it["fødselsdato"].asString() shouldBe "2000-01-01"
            it["statsborgerskap"].asString() shouldBe "UTL"
            it["kvalifiserer"].asBoolean() shouldBe true
        }
    }

    @Test
    fun `løser behov om barn som forventet hvis søknaden ikke har barn`() {
        val søknadstidspunkt = ZonedDateTime.now()
        val søknadstidpsunktOpplysning =
            QuizOpplysning(
                beskrivendeId = "søknadstidspunkt",
                type = Tekst,
                svar = søknadstidspunkt.toString(),
                ident = ident,
                søknadId = søknadId,
            )

        opplysningRepository.lagre(søknadstidpsunktOpplysning)

        behovløser.løs(lagBehovmelding(ident, søknadId, BarnetilleggV2))

        val barnetilleggV2Løsning = testRapid.inspektør.field(0, "@løsning")[BarnetilleggV2.name]["verdi"]
        val løsteBarn = barnetilleggV2Løsning["barn"]
        verify(exactly = 1) { quizOpplysningRepositorySpy.hentEllerOpprettSøknadbarnId(any()) }
        barnetilleggV2Løsning["søknadbarnId"] shouldNotBe null
        løsteBarn.shouldBeEmpty()
    }

    @Test
    fun `Løser behov med data fra seksjonV2`() {
        val søknadstidspunkt = ZonedDateTime.now()
        val søknadstidpsunktOpplysning =
            QuizOpplysning(
                beskrivendeId = "søknadstidspunkt",
                type = Tekst,
                svar = søknadstidspunkt.toString(),
                ident = ident,
                søknadId = søknadId,
            )
        opplysningRepository.lagre(søknadstidpsunktOpplysning)
        every { seksjonRepository.hentSeksjonsvar(any(), any(), any()) } returns
            barnetilleggseksjonsvar.trimIndent()

        behovløser.løs(lagBehovmelding(ident, søknadId, BarnetilleggV2))

        verify(exactly = 1) { seksjonRepository.hentSeksjonsvar(søknadId, ident, "barnetillegg") }
        val barnetilleggV2Løsning = testRapid.inspektør.field(0, "@løsning")[BarnetilleggV2.name]["verdi"]
        val løsteBarn = barnetilleggV2Løsning["barn"]

        barnetilleggV2Løsning["søknadbarnId"] shouldNotBe null
        løsteBarn.size() shouldBe 3
        løsteBarn[0].also {
            it["fornavnOgMellomnavn"].asString() shouldBe "SMISKENDE"
            it["etternavn"].asString() shouldBe "KJENNING"
            it["fødselsdato"].asString() shouldBe "2013-05-26"
            it["statsborgerskap"].asString() shouldBe "NOR"
            it["kvalifiserer"].asBoolean() shouldBe false
            it["barnetilleggFom"].isNull shouldBe true
            it["barnetilleggTom"].isNull shouldBe true
            it["endretAv"].isNull shouldBe true
            it["begrunnelse"].isNull shouldBe true
        }
        løsteBarn[1].also {
            it["kvalifiserer"].asBoolean() shouldBe true
            it["barnetilleggFom"].asString() shouldBe "2009-11-12"
            it["barnetilleggTom"].asString() shouldBe "2027-11-12"
        }
        løsteBarn[2].also {
            it["kvalifiserer"].asBoolean() shouldBe false
            it["barnetilleggFom"].isNull shouldBe true
            it["barnetilleggTom"].isNull shouldBe true
        }
    }

    @Test
    fun `saksbehandler-redigerte barn har prioritet over quiz-opplysninger`() {
        val saksbehandlerBarn =
            listOf(
                BarnSvar(
                    barnSvarId = randomUUID(),
                    fornavnOgMellomnavn = "Saksbehandler-redigert",
                    etternavn = "Barn",
                    fødselsdato = 1.januar(2010),
                    statsborgerskap = "NOR",
                    forsørgerBarnet = true,
                    fraRegister = true,
                    kvalifisererTilBarnetillegg = true,
                    barnetilleggFom = 1.januar(2010),
                    barnetilleggTom = 1.januar(2028),
                    endretAv = "Z991234",
                    begrunnelse = "Endret av saksbehandler",
                ),
            )
        every { saksbehandlerBarnRepository.hentBarn(søknadId) } returns saksbehandlerBarn

        // Legg til quiz-barn som IKKE skal brukes
        val pdlBarn =
            QuizOpplysning(
                beskrivendeId = BESKRIVENDE_ID_PDL_BARN,
                type = Barn,
                ident = ident,
                søknadId = søknadId,
                svar =
                    listOf(
                        BarnSvar(
                            barnSvarId = randomUUID(),
                            fornavnOgMellomnavn = "Quiz",
                            etternavn = "Barn",
                            fødselsdato = 1.januar(2000),
                            statsborgerskap = "NOR",
                            forsørgerBarnet = false,
                            fraRegister = true,
                            kvalifisererTilBarnetillegg = false,
                        ),
                    ),
            )
        opplysningRepository.lagre(pdlBarn)

        behovløser.løs(lagBehovmelding(ident, søknadId, BarnetilleggV2))

        val barnetilleggV2Løsning = testRapid.inspektør.field(0, "@løsning")[BarnetilleggV2.name]["verdi"]
        val løsteBarn = barnetilleggV2Løsning["barn"]
        løsteBarn.size() shouldBe 1
        løsteBarn[0]["fornavnOgMellomnavn"].asString() shouldBe "Saksbehandler-redigert"
        løsteBarn[0]["kvalifiserer"].asBoolean() shouldBe true
        løsteBarn[0]["endretAv"].asString() shouldBe "Z991234"
        løsteBarn[0]["begrunnelse"].asString() shouldBe "Endret av saksbehandler"
    }

    @Language("JSON")
    val barnetilleggseksjonsvar = """
{
  "barnFraPdl": [
    {
      "id": "b6d35e04-f34f-4713-8972-5e8e2a9a40ed",
      "fornavn": "SMISKENDE",
      "mellomnavn": "",
      "fornavnOgMellomnavn": "SMISKENDE",
      "etternavn": "KJENNING",
      "fødselsdato": "2013-05-26",
      "alder": 12,
      "bostedsland": "NOR",
      "forsørgerDuBarnet": "nei"
    },
    {
      "id": "c145d5e9-ff51-47a0-b393-2752bf17855f",
      "fornavn": "ENGASJERT",
      "mellomnavn": "",
      "fornavnOgMellomnavn": "ENGASJERT",
      "etternavn": "BUSSTOPP",
      "fødselsdato": "2009-11-12",
      "alder": 16,
      "bostedsland": "NOR",
      "forsørgerDuBarnet": "ja"
    }
  ],
  "forsørgerDuBarnSomIkkeVisesHer": "ja",
  "barnLagtManuelt": [
    {
      "id": "601b9124-183f-4e9a-a3a3-c49389451255",
      "dokumentasjonskravId": "fe1024f5-7f62-4b7a-94c9-b9cc38205432",
      "fornavnOgMellomnavn": "l",
      "etternavn": "l",
      "fødselsdato": "2025-10-21",
      "bostedsland": "ARG"
    }
  ]
}
            """
}
