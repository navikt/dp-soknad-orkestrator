package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.Lønnsgaranti
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
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.test.Test

class LønnsgarantiBehovløserTest {
    val opplysningRepository = InMemoryQuizOpplysningRepository()
    val testRapid = TestRapid()
    val søknadRepository = mockk<SøknadRepository>(relaxed = true)
    val seksjonRepository = mockk<SeksjonRepository>(relaxed = true)
    val behovløser = LønnsgarantiBehovløser(testRapid, opplysningRepository, søknadRepository, seksjonRepository)
    val ident = "12345678910"
    val søknadId = UUID.randomUUID()

    @Test
    fun `Behovløser publiserer løsning på behov Lønnsgaranti med verdi og gjelderFra`() {
        opplysningRepository.lagre(opplysning())

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

        opplysningRepository.lagre(søknadstidpsunktOpplysning)
        behovløser.løs(lagBehovmelding(ident, søknadId, Lønnsgaranti))

        testRapid.inspektør.message(0)["@løsning"]["Lønnsgaranti"].also { løsning ->
            løsning["verdi"].asBoolean() shouldBe true
            løsning["gjelderFra"].asLocalDate() shouldBe søknadstidspunkt.toLocalDate()
        }
    }

    @Test
    fun `Behovløser publiserer løsning på behov Lønnsgaranti med verdi og gjelderFra fra seksjonsdata`() {
        every {
            seksjonRepository.hentSeksjonsvar(
                any(),
                any(),
                any(),
            )
        } returns
            """
            {
                "seksjonId": "arbeidsforhold",
                "seksjon": {
                "registrerteArbeidsforhold": [
                    {
                        "hvordan-har-dette-arbeidsforholdet-endret-seg": "arbeidsgiver-er-konkurs"
                    }
                ]
                },
                "versjon": 1
            }
            """.trimIndent()

        // Må også lagre søknadstidspunkt fordi det er denne som brukes for å sette gjelderFra i første omgang
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
        behovløser.løs(lagBehovmelding(ident, søknadId, Lønnsgaranti))

        verify { seksjonRepository.hentSeksjonsvar(ident, søknadId, "arbeidsforhold") }
        verify {
            søknadRepository.hent(søknadId)
        }
        testRapid.inspektør.message(0)["@løsning"]["Lønnsgaranti"].also { løsning ->
            løsning["verdi"].asBoolean() shouldBe true
            løsning["gjelderFra"].asLocalDate() shouldBe søknadstidspunkt.toLocalDate()
        }
    }

    @Suppress("ktlint:standard:max-line-length")
    @Test
    fun `Behovløser publiserer løsning på behov Lønnsgaranti med verdi og gjelderFra fra seksjonsdata hvor arbeidsgiveren ikke gikk konkurs`() {
        every {
            seksjonRepository.hentSeksjonsvar(
                any(),
                any(),
                any(),
            )
        } returns
            """
            {
                "seksjonId": "arbeidsforhold",
                "seksjon": {
                "registrerteArbeidsforhold": [
                    {
                        "hvordan-har-dette-arbeidsforholdet-endret-seg": "jeg-har-fått-avskjed"
                    }
                ]
                },
                "versjon": 1
            }
            """.trimIndent()

        // Må også lagre søknadstidspunkt fordi det er denne som brukes for å sette gjelderFra i første omgang
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
        behovløser.løs(lagBehovmelding(ident, søknadId, Lønnsgaranti))

        verify { seksjonRepository.hentSeksjonsvar(ident, søknadId, "arbeidsforhold") }
        verify {
            søknadRepository.hent(søknadId)
        }
        testRapid.inspektør.message(0)["@løsning"]["Lønnsgaranti"].also { løsning ->
            løsning["verdi"].asBoolean() shouldBe false
            løsning["gjelderFra"].asLocalDate() shouldBe søknadstidspunkt.toLocalDate()
        }
    }

    @Suppress("ktlint:standard:max-line-length")
    @Test
    fun `Behovløser publiserer løsning på behov Lønnsgaranti med verdi og gjelderFra fra seksjonsdata uten arbeidsforhold svarer false`() {
        every {
            seksjonRepository.hentSeksjonsvar(
                any(),
                any(),
                any(),
            )
        } returns
            """
            {
                "seksjonId": "arbeidsforhold",
                "seksjon": {
                "registrerteArbeidsforhold": []
                },
                "versjon": 1
            }
            """.trimIndent()

        // Må også lagre søknadstidspunkt fordi det er denne som brukes for å sette gjelderFra i første omgang
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
        behovløser.løs(lagBehovmelding(ident, søknadId, Lønnsgaranti))

        verify { seksjonRepository.hentSeksjonsvar(ident, søknadId, "arbeidsforhold") }
        verify {
            søknadRepository.hent(søknadId)
        }
        testRapid.inspektør.message(0)["@løsning"]["Lønnsgaranti"].also { løsning ->
            løsning["verdi"].asBoolean() shouldBe false
            løsning["gjelderFra"].asLocalDate() shouldBe søknadstidspunkt.toLocalDate()
        }
    }

    @Test
    fun `Behovløser setter løsning til true når minst 1 arbeidsforhold har sluttårsak ARBEIDSGIVER_KONKURS`() {
        val svarMedEttKonkursArbeidsforhold =
            listOf(
                ArbeidsforholdSvar(
                    navn = "arbeidsforhold1",
                    land = "NOR",
                    sluttårsak = Sluttårsak.ARBEIDSGIVER_KONKURS,
                ),
                ArbeidsforholdSvar(
                    navn = "arbeidsforhold2",
                    land = "NOR",
                    sluttårsak = Sluttårsak.KONTRAKT_UTGAATT,
                ),
            )

        opplysningRepository.lagre(opplysning(svar = svarMedEttKonkursArbeidsforhold))
        behovløser.rettTilDagpengerEtterKonkurs(ident, søknadId) shouldBe true
    }

    @Test
    fun `Behovløser setter løsning til false når ingen arbeidsforhold har sluttårsak ARBEIDSGIVER_KONKURS`() {
        val svarUtenKonkursArbeidsforhold =
            listOf(
                ArbeidsforholdSvar(
                    navn = "arbeidsforhold",
                    land = "NOR",
                    sluttårsak = Sluttårsak.PERMITTERT,
                ),
            )

        opplysningRepository.lagre(opplysning(svar = svarUtenKonkursArbeidsforhold))
        behovløser.rettTilDagpengerEtterKonkurs(ident, søknadId) shouldBe false
    }

    @Test
    fun `Behovløser setter løsning til false når det ikke er noen opplysning om arbeidsforhold`() {
        behovløser.rettTilDagpengerEtterKonkurs(ident, søknadId) shouldBe false
    }

    private fun opplysning(
        svar: List<ArbeidsforholdSvar> =
            listOf(
                ArbeidsforholdSvar(
                    navn = "arbeidsforhold",
                    land = "NOR",
                    sluttårsak = Sluttårsak.ARBEIDSGIVER_KONKURS,
                ),
            ),
    ): QuizOpplysning<List<ArbeidsforholdSvar>> {
        val opplysning =
            QuizOpplysning(
                beskrivendeId = behovløser.beskrivendeId,
                type = Arbeidsforhold,
                svar = svar,
                ident = ident,
                søknadId = søknadId,
            )
        return opplysning
    }
}
