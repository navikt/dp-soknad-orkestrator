package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.spyk
import io.mockk.verify
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.BarnetilleggV2
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.BarnetilleggBehovLøser.Companion.BESKRIVENDE_ID_EGNE_BARN
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.BarnetilleggBehovLøser.Companion.BESKRIVENDE_ID_PDL_BARN
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.QuizOpplysning
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Barn
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.BarnSvar
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.utils.InMemoryQuizOpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.utils.asUUID
import no.nav.dagpenger.soknad.orkestrator.utils.januar
import org.junit.jupiter.api.Test
import java.util.UUID
import java.util.UUID.randomUUID

class BarnetilleggV2BehovLøserTest {
    val opplysningRepository = InMemoryQuizOpplysningRepository()
    val quizOpplysningRepositorySpy = spyk<QuizOpplysningRepository>(opplysningRepository)
    val testRapid = TestRapid()
    val behovløser = BarnetilleggV2BehovLøser(testRapid, quizOpplysningRepositorySpy)
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
        opplysningRepository.lagre(pdlBarn)
        opplysningRepository.lagre(egetBarn)
        val lagretSøknadbarnId = opplysningRepository.lagreBarnSøknadMapping(søknadId = søknadId)

        behovløser.løs(lagBehovmelding(ident, søknadId, BarnetilleggV2))

        val barnetilleggV2Løsning = testRapid.inspektør.field(0, "@løsning")[BarnetilleggV2.name]["verdi"]
        val løsteBarn = barnetilleggV2Løsning["barn"]
        verify(exactly = 1) { quizOpplysningRepositorySpy.hentEllerOpprettSøknadbarnId(any()) }
        barnetilleggV2Løsning["søknadbarnId"].asUUID() shouldBe lagretSøknadbarnId
        løsteBarn.size() shouldBe 3
        løsteBarn[0].also {
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
            it["fornavnOgMellomnavn"].asText() shouldBe "Per"
            it["etternavn"].asText() shouldBe "Nordmann"
            it["fødselsdato"].asText() shouldBe "2000-01-01"
            it["statsborgerskap"].asText() shouldBe "NOR"
            it["kvalifiserer"].asBoolean() shouldBe false
        }
        løsteBarn[2].also {
            it["fornavnOgMellomnavn"].asText() shouldBe "Per"
            it["etternavn"].asText() shouldBe "Utland"
            it["fødselsdato"].asText() shouldBe "2000-01-01"
            it["statsborgerskap"].asText() shouldBe "UTL"
            it["kvalifiserer"].asBoolean() shouldBe true
        }
    }

    @Test
    fun `løser behov om barn som forventet hvis søknaden ikke har barn`() {
        behovløser.løs(lagBehovmelding(ident, søknadId, BarnetilleggV2))

        val barnetilleggV2Løsning = testRapid.inspektør.field(0, "@løsning")[BarnetilleggV2.name]["verdi"]
        val løsteBarn = barnetilleggV2Løsning["barn"]
        verify(exactly = 1) { quizOpplysningRepositorySpy.hentEllerOpprettSøknadbarnId(any()) }
        barnetilleggV2Løsning["søknadbarnId"] shouldNotBe null
        løsteBarn.shouldBeEmpty()
    }
}
