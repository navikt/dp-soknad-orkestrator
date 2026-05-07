package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.BarnOver16
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.BarnOver16Behovløser.Companion.BESKRIVENDE_ID_EGNE_BARN
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.BarnOver16Behovløser.Companion.BESKRIVENDE_ID_PDL_BARN
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.QuizOpplysning
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Barn
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.BarnSvar
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Tekst
import no.nav.dagpenger.soknad.orkestrator.søknad.Søknad
import no.nav.dagpenger.soknad.orkestrator.søknad.Tilstand
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonRepository
import no.nav.dagpenger.soknad.orkestrator.utils.InMemoryQuizOpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.utils.januar
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID
import java.util.UUID.randomUUID

class BarnOver16BehovløserTest {
    val opplysningRepository = InMemoryQuizOpplysningRepository()
    val søknadRepository = mockk<SøknadRepository>(relaxed = true)
    val seksjonRepository = mockk<SeksjonRepository>(relaxed = true)
    val testRapid = TestRapid()
    val behovløser = BarnOver16Behovløser(testRapid, opplysningRepository, søknadRepository, seksjonRepository)
    val ident = "12345678910"
    val søknadId: UUID = randomUUID()

    @Test
    fun `skal returnere true når PDL-barn er over 16 år`() {
        opplysningRepository.lagre(
            QuizOpplysning(
                beskrivendeId = BESKRIVENDE_ID_PDL_BARN,
                type = Barn,
                ident = ident,
                søknadId = søknadId,
                svar = listOf(barnSvar(fødselsdato = LocalDate.now().minusYears(17))),
            ),
        )

        behovløser.harBarnOver16(ident, søknadId) shouldBe true
    }

    @Test
    fun `skal returnere false når PDL-barn er under 16 år`() {
        opplysningRepository.lagre(
            QuizOpplysning(
                beskrivendeId = BESKRIVENDE_ID_PDL_BARN,
                type = Barn,
                ident = ident,
                søknadId = søknadId,
                svar = listOf(barnSvar(fødselsdato = LocalDate.now().minusYears(10))),
            ),
        )

        behovløser.harBarnOver16(ident, søknadId) shouldBe false
    }

    @Test
    fun `skal returnere true når egne barn er over 16 år`() {
        opplysningRepository.lagre(
            QuizOpplysning(
                beskrivendeId = BESKRIVENDE_ID_EGNE_BARN,
                type = Barn,
                ident = ident,
                søknadId = søknadId,
                svar = listOf(barnSvar(fødselsdato = LocalDate.now().minusYears(18))),
            ),
        )

        behovløser.harBarnOver16(ident, søknadId) shouldBe true
    }

    @Test
    fun `skal returnere false når ingen barn finnes i quiz-opplysninger og seksjon returnerer null`() {
        every { seksjonRepository.hentSeksjonsvar(any(), any(), any()) } returns null

        behovløser.harBarnOver16(ident, søknadId) shouldBe false
    }

    @Test
    fun `skal returnere false når barnFraPdl og barnLagtManuelt er null i seksjonsdata`() {
        every { seksjonRepository.hentSeksjonsvar(søknadId, ident, "barnetillegg") } returns
            """
            {
              "seksjonId": "barnetillegg",
              "seksjonsvar": {
                "barnFraPdl": null,
                "barnLagtManuelt": null
              },
              "versjon": 1
            }
            """.trimIndent()

        behovløser.harBarnOver16(ident, søknadId) shouldBe false
    }

    @Test
    fun `skal returnere true når barn i seksjonsdata er over 16 år`() {
        val fødselsdato = LocalDate.now().minusYears(17).toString()
        every { seksjonRepository.hentSeksjonsvar(søknadId, ident, "barnetillegg") } returns
            """
            {
              "seksjonId": "barnetillegg",
              "seksjonsvar": {
                "barnFraPdl": [
                  { "fødselsdato": "$fødselsdato" }
                ],
                "barnLagtManuelt": []
              },
              "versjon": 1
            }
            """.trimIndent()

        behovløser.harBarnOver16(ident, søknadId) shouldBe true
    }

    @Test
    fun `skal returnere false når barn i seksjonsdata er under 16 år`() {
        val fødselsdato = LocalDate.now().minusYears(5).toString()
        every { seksjonRepository.hentSeksjonsvar(søknadId, ident, "barnetillegg") } returns
            """
            {
              "seksjonId": "barnetillegg",
              "seksjonsvar": {
                "barnFraPdl": [
                  { "fødselsdato": "$fødselsdato" }
                ],
                "barnLagtManuelt": []
              },
              "versjon": 1
            }
            """.trimIndent()

        behovløser.harBarnOver16(ident, søknadId) shouldBe false
    }

    @Test
    fun `skal publisere løsning på behov BarnOver16 med verdi og gjelderFra`() {
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
            QuizOpplysning(
                beskrivendeId = BESKRIVENDE_ID_PDL_BARN,
                type = Barn,
                ident = ident,
                søknadId = søknadId,
                svar = listOf(barnSvar(fødselsdato = LocalDate.now().minusYears(17))),
            ),
        )

        behovløser.løs(lagBehovmelding(ident, søknadId, BarnOver16))

        testRapid.inspektør.message(0)["@løsning"]["BarnOver16"].also { løsning ->
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
        val fødselsdato = LocalDate.now().minusYears(17).toString()
        every { seksjonRepository.hentSeksjonsvar(søknadId, ident, "barnetillegg") } returns
            """
            {
              "seksjonId": "barnetillegg",
              "seksjonsvar": {
                "barnFraPdl": [{ "fødselsdato": "$fødselsdato" }],
                "barnLagtManuelt": []
              },
              "versjon": 1
            }
            """.trimIndent()

        behovløser.løs(lagBehovmelding(ident, søknadId, BarnOver16))

        testRapid.inspektør.message(0)["@løsning"]["BarnOver16"].also { løsning ->
            løsning["verdi"].asBoolean() shouldBe true
            løsning["gjelderFra"].asLocalDate() shouldBe innsendtTidspunkt.toLocalDate()
        }
    }

    private fun barnSvar(fødselsdato: LocalDate) =
        BarnSvar(
            barnSvarId = randomUUID(),
            fornavnOgMellomnavn = "Test",
            etternavn = "Testesen",
            fødselsdato = fødselsdato,
            statsborgerskap = "NOR",
            forsørgerBarnet = true,
            fraRegister = true,
            kvalifisererTilBarnetillegg = true,
            barnetilleggFom = 1.januar(2010),
            barnetilleggTom = 1.januar(2028),
            endretAv = null,
            begrunnelse = null,
        )
}
