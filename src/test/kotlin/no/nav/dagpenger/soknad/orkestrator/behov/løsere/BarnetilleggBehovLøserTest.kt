package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.Barnetillegg
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.BarnetilleggBehovLøser.Companion.beskrivendeIdEgneBarn
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.BarnetilleggBehovLøser.Companion.beskrivendeIdPdlBarn
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.QuizOpplysning
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Barn
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.BarnSvar
import no.nav.dagpenger.soknad.orkestrator.utils.InMemoryQuizOpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.utils.januar
import org.junit.jupiter.api.Test
import java.util.UUID

class BarnetilleggBehovLøserTest {
    val opplysningRepository = InMemoryQuizOpplysningRepository()
    val testRapid = TestRapid()
    val behovløser = BarnetilleggBehovLøser(testRapid, opplysningRepository)
    val ident = "12345678910"
    val søknadId = UUID.randomUUID()

    @Test
    fun `løser behov om barn`() {
        val pdlBarn =
            QuizOpplysning(
                beskrivendeId = beskrivendeIdPdlBarn,
                type = Barn,
                ident = ident,
                søknadId = søknadId,
                svar =
                    listOf(
                        BarnSvar(
                            barnSvarId = UUID.randomUUID(),
                            fornavnOgMellomnavn = "Ola",
                            etternavn = "Nordmann",
                            fødselsdato = 1.januar(2000),
                            statsborgerskap = "NOR",
                            forsørgerBarnet = true,
                            fraRegister = true,
                            kvalifisererTilBarnetillegg = true,
                        ),
                        BarnSvar(
                            barnSvarId = UUID.randomUUID(),
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
                beskrivendeId = beskrivendeIdEgneBarn,
                type = Barn,
                ident = ident,
                søknadId = søknadId,
                svar =
                    listOf(
                        BarnSvar(
                            barnSvarId = UUID.randomUUID(),
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
        behovløser.løs(lagBehovmelding(ident, søknadId, Barnetillegg))

        val løsteBarn = testRapid.inspektør.field(0, "@løsning")[Barnetillegg.name]["verdi"]
        løsteBarn.size() shouldBe 3
        løsteBarn[0].also {
            it["fornavnOgMellomnavn"].asText() shouldBe "Ola"
            it["etternavn"].asText() shouldBe "Nordmann"
            it["fødselsdato"].asText() shouldBe "2000-01-01"
            it["statsborgerskap"].asText() shouldBe "NOR"
            it["kvalifiserer"].asBoolean() shouldBe true
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
            it["kvalifiserer"].asBoolean() shouldBe false
        }
    }
}
