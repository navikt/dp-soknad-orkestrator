package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.Barnetillegg
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.BarnetilleggBehovLøser.Companion.BESKRIVENDE_ID_EGNE_BARN
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.BarnetilleggBehovLøser.Companion.BESKRIVENDE_ID_PDL_BARN
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
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.util.UUID
import java.util.UUID.randomUUID

class BarnetilleggBehovLøserTest {
    val opplysningRepository = InMemoryQuizOpplysningRepository()
    val quizOpplysningRepositorySpy = spyk<QuizOpplysningRepository>(opplysningRepository)
    val seksjonRepository = mockk<SeksjonRepository>()
    val søknadRepository = mockk<SøknadRepository>(relaxed = true)
    val testRapid = TestRapid()
    val behovløser = BarnetilleggBehovLøser(testRapid, quizOpplysningRepositorySpy, søknadRepository, seksjonRepository)
    val ident = "12345678910"
    val søknadId: UUID = randomUUID()

    @Test
    fun `løser behov om barn hvis søknaden har barn`() {
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

        behovløser.løs(lagBehovmelding(ident, søknadId, Barnetillegg))

        verify(exactly = 1) { quizOpplysningRepositorySpy.hentEllerOpprettSøknadbarnId(any()) }
        val løsteBarn = testRapid.inspektør.field(0, "@løsning")[Barnetillegg.name]["verdi"]
        løsteBarn.size() shouldBe 3
        løsteBarn[0].also {
            it["søknadbarnId"].asUUID() shouldBe lagretSøknadbarnId
            it["fornavnOgMellomnavn"].asText() shouldBe "Ola"
            it["etternavn"].asText() shouldBe "Nordmann"
            it["fødselsdato"].asText() shouldBe "2000-01-01"
            it["statsborgerskap"].asText() shouldBe "NOR"
            it["kvalifiserer"].asBoolean() shouldBe true
            it["barnetilleggFom"].asLocalDate() shouldBe 1.januar(2000)
            it["barnetilleggTom"].asLocalDate() shouldBe 1.januar(2018)
            it["endretAv"].asText() shouldBe "123"
            it["begrunnelse"].asText() shouldBe "Begrunnelse for endring"
        }
        løsteBarn[1].also {
            it["søknadbarnId"].asUUID() shouldBe lagretSøknadbarnId
            it["fornavnOgMellomnavn"].asText() shouldBe "Per"
            it["etternavn"].asText() shouldBe "Nordmann"
            it["fødselsdato"].asText() shouldBe "2000-01-01"
            it["statsborgerskap"].asText() shouldBe "NOR"
            it["kvalifiserer"].asBoolean() shouldBe false
        }
        løsteBarn[2].also {
            it["søknadbarnId"].asUUID() shouldBe lagretSøknadbarnId
            it["fornavnOgMellomnavn"].asText() shouldBe "Per"
            it["etternavn"].asText() shouldBe "Utland"
            it["fødselsdato"].asText() shouldBe "2000-01-01"
            it["statsborgerskap"].asText() shouldBe "UTL"
            it["kvalifiserer"].asBoolean() shouldBe true
        }
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

        behovløser.løs(lagBehovmelding(ident, søknadId, Barnetillegg))

        verify(exactly = 1) { seksjonRepository.hentSeksjonsvar(ident, søknadId, "barnetillegg") }
        val løsteBarn = testRapid.inspektør.field(0, "@løsning")[Barnetillegg.name]["verdi"]
        løsteBarn.size() shouldBe 3
        løsteBarn[0].also {
            it["søknadbarnId"].asUUID().shouldNotBeNull()
            it["fornavnOgMellomnavn"].asText() shouldBe "SMISKENDE"
            it["etternavn"].asText() shouldBe "KJENNING"
            it["fødselsdato"].asText() shouldBe "2013-05-26"
            it["statsborgerskap"].asText() shouldBe "NOR"
            it["kvalifiserer"].asBoolean() shouldBe false
            it["barnetilleggFom"].asText().shouldBe("null")
            it["barnetilleggTom"].asText().shouldBe("null")
            it["endretAv"].asText().shouldBe("null")
            it["begrunnelse"].asText().shouldBe("null")
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
        every { seksjonRepository.hentSeksjonsvar(any(), any(), any()) } returns null

        behovløser.løs(lagBehovmelding(ident, søknadId, Barnetillegg))

        val løsteBarn = testRapid.inspektør.field(0, "@løsning")[Barnetillegg.name]["verdi"]
        verify(exactly = 0) { quizOpplysningRepositorySpy.hentEllerOpprettSøknadbarnId(any()) }
        løsteBarn.shouldBeEmpty()
    }
}

val barnetilleggseksjonsvar = """
            {
              "barnFraPdl": [
                {
                  "fornavn-og-mellomnavn": "SMISKENDE",
                  "etternavn": "KJENNING",
                  "fødselsdato": "2013-05-26",
                  "bostedsland": "NOR",
                  "forsørger-du-barnet": "ja"
                },
                {
                  "fornavn-og-mellomnavn": "ENGASJERT",
                  "etternavn": "BUSSTOPP",
                  "fødselsdato": "2009-11-12",
                  "bostedsland": "NOR",
                  "forsørger-du-barnet": "ja"
                }
              ],
              "forsørger-du-barn-som-ikke-vises-her": "ja",
              "barnLagtManuelt": [
                {
                  "id": "601b9124-183f-4e9a-a3a3-c49389451255",
                  "dokumentasjonskravId": "fe1024f5-7f62-4b7a-94c9-b9cc38205432",
                  "fornavn-og-mellomnavn": "l",
                  "etternavn": "l",
                  "fødselsdato": "2025-10-21",
                  "bostedsland": "ARG"
                }
              ]
            }
            """
