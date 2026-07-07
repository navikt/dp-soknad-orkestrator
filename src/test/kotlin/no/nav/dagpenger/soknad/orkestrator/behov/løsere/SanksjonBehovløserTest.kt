package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.Sanksjon
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.QuizOpplysning
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Arbeidsforhold
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.ArbeidsforholdSvar
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Sluttårsak
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Tekst
import no.nav.dagpenger.soknad.orkestrator.søknad.Søknad
import no.nav.dagpenger.soknad.orkestrator.søknad.Tilstand
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonRepository
import no.nav.dagpenger.soknad.orkestrator.utils.InMemoryQuizOpplysningRepository
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.util.UUID

class SanksjonBehovløserTest {
    val opplysningRepository = InMemoryQuizOpplysningRepository()
    val søknadRepository = mockk<SøknadRepository>(relaxed = true)
    val seksjonRepository = mockk<SeksjonRepository>(relaxed = true)
    val testRapid = TestRapid()
    val behovløser = SanksjonBehovløser(testRapid, opplysningRepository, søknadRepository, seksjonRepository)
    val ident = "12345678910"
    val søknadId: UUID = UUID.randomUUID()

    @Test
    fun `skal returnere true når minst ett arbeidsforhold har sanksjonsluttårsak fra quiz`() {
        opplysningRepository.lagre(
            arbeidsforholdOpplysning(
                listOf(
                    ArbeidsforholdSvar(navn = "Arbeidsgiver AS", land = "NOR", sluttårsak = Sluttårsak.SAGT_OPP_SELV),
                    ArbeidsforholdSvar(navn = "Annen AS", land = "NOR", sluttårsak = Sluttårsak.PERMITTERT),
                ),
            ),
        )

        behovløser.harSanksjon(ident, søknadId) shouldBe true
    }

    @Test
    fun `skal returnere true ved AVSKJEDIGET`() {
        opplysningRepository.lagre(
            arbeidsforholdOpplysning(
                listOf(ArbeidsforholdSvar(navn = "Arbeidsgiver AS", land = "NOR", sluttårsak = Sluttårsak.AVSKJEDIGET)),
            ),
        )

        behovløser.harSanksjon(ident, søknadId) shouldBe true
    }

    @Test
    fun `skal returnere true ved SAGT_OPP_SELV`() {
        opplysningRepository.lagre(
            arbeidsforholdOpplysning(
                listOf(ArbeidsforholdSvar(navn = "Arbeidsgiver AS", land = "NOR", sluttårsak = Sluttårsak.SAGT_OPP_SELV)),
            ),
        )

        behovløser.harSanksjon(ident, søknadId) shouldBe true
    }

    @Test
    fun `skal returnere false når ingen arbeidsforhold har sanksjonsluttårsak fra quiz`() {
        opplysningRepository.lagre(
            arbeidsforholdOpplysning(
                listOf(
                    ArbeidsforholdSvar(navn = "Arbeidsgiver AS", land = "NOR", sluttårsak = Sluttårsak.PERMITTERT),
                    ArbeidsforholdSvar(navn = "Annen AS", land = "NOR", sluttårsak = Sluttårsak.KONTRAKT_UTGAATT),
                ),
            ),
        )

        behovløser.harSanksjon(ident, søknadId) shouldBe false
    }

    @Test
    fun `skal returnere false når verken quiz-opplysning eller arbeidsforhold-seksjon finnes`() {
        every {
            seksjonRepository.hentSeksjonsvar(any(), any(), any())
        } returns null

        behovløser.harSanksjon(ident, søknadId) shouldBe false
    }

    @Test
    fun `skal returnere true ved sanksjonsluttårsak i seksjonsdata`() {
        every {
            seksjonRepository.hentSeksjonsvar(søknadId, ident, "arbeidsforhold")
        } returns
            """
            {
              "seksjonId": "arbeidsforhold",
              "seksjonsvar": {
                "registrerteArbeidsforhold": [
                  { "hvordanHarDetteArbeidsforholdetEndretSeg": "jegHarSagtOppSelv" }
                ]
              },
              "versjon": 1
            }
            """.trimIndent()

        behovløser.harSanksjon(ident, søknadId) shouldBe true
    }

    @Test
    fun `skal returnere true hvis søkeren har takket nei til tilbud om fortsettelse`() {
        every {
            seksjonRepository.hentSeksjonsvar(søknadId, ident, "arbeidsforhold")
        } returns
            """
            {
              "seksjonId": "arbeidsforhold",
              "seksjonsvar": {
                "registrerteArbeidsforhold": [
                  { "hvordanHarDetteArbeidsforholdetEndretSeg": "jegErOppsagt", "jegErOppsagtHvaHarDuSvartPåTilbudet": "nei" }
                ]
              },
              "versjon": 1
            }
            """.trimIndent()

        behovløser.harSanksjon(ident, søknadId) shouldBe true
    }

    @Test
    fun `skal returnere false når ingen arbeidsforhold har sanksjonsluttårsak i seksjonsdata`() {
        every {
            seksjonRepository.hentSeksjonsvar(søknadId, ident, "arbeidsforhold")
        } returns
            """
            {
              "seksjonId": "arbeidsforhold",
              "seksjonsvar": {
                "registrerteArbeidsforhold": [
                  { "hvordanHarDetteArbeidsforholdetEndretSeg": "permittert" }
                ]
              },
              "versjon": 1
            }
            """.trimIndent()

        behovløser.harSanksjon(ident, søknadId) shouldBe false
    }

    @Test
    fun `skal publisere løsning på behov Sanksjon med verdi og gjelderFra`() {
        val søknadstidspunkt = ZonedDateTime.now()
        opplysningRepository.lagre(
            QuizOpplysning(
                beskrivendeId = "søknadstidspunkt",
                type = Tekst,
                svar = søknadstidspunkt.toString(),
                ident = ident,
                søknadId = søknadId,
            ),
        )
        opplysningRepository.lagre(
            arbeidsforholdOpplysning(
                listOf(ArbeidsforholdSvar(navn = "AS", land = "NOR", sluttårsak = Sluttårsak.SAGT_OPP_SELV)),
            ),
        )

        behovløser.løs(lagBehovmelding(ident, søknadId, Sanksjon))

        testRapid.inspektør.message(0)["@løsning"]["Sanksjon"].also { løsning ->
            løsning["verdi"].asBoolean() shouldBe true
            løsning["gjelderFra"].asLocalDate() shouldBe søknadstidspunkt.toLocalDate()
        }
    }

    @Test
    fun `skal publisere løsning fra seksjonsdata med gjelderFra fra søknadRepository`() {
        val innsendtTidspunkt = ZonedDateTime.now()
        every { søknadRepository.hent(søknadId) } returns
            Søknad(
                søknadId = søknadId,
                ident = ident,
                tilstand = Tilstand.INNSENDT,
                innsendtTidspunkt = innsendtTidspunkt.toLocalDateTime(),
            )
        every {
            seksjonRepository.hentSeksjonsvar(søknadId, ident, "arbeidsforhold")
        } returns
            """
            {
              "seksjonId": "arbeidsforhold",
              "seksjonsvar": {
                "registrerteArbeidsforhold": [
                  { "hvordanHarDetteArbeidsforholdetEndretSeg": "jegHarSagtOppSelv" }
                ]
              },
              "versjon": 1
            }
            """.trimIndent()

        behovløser.løs(lagBehovmelding(ident, søknadId, Sanksjon))

        testRapid.inspektør.message(0)["@løsning"]["Sanksjon"].also { løsning ->
            løsning["verdi"].asBoolean() shouldBe true
            løsning["gjelderFra"].asLocalDate() shouldBe innsendtTidspunkt.toLocalDate()
        }
    }

    private fun arbeidsforholdOpplysning(svar: List<ArbeidsforholdSvar>) =
        QuizOpplysning(
            beskrivendeId = behovløser.beskrivendeId,
            type = Arbeidsforhold,
            svar = svar,
            ident = ident,
            søknadId = søknadId,
        )
}
