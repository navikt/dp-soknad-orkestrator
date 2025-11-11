package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.Permittert
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

class PermittertBehovløserTest {
    val opplysningRepository = InMemoryQuizOpplysningRepository()
    val testRapid = TestRapid()
    val søknadRepository = mockk<SøknadRepository>(relaxed = true)
    val seksjonRepository = mockk<SeksjonRepository>(relaxed = true)
    val behovløser = PermittertBehovløser(testRapid, opplysningRepository, søknadRepository, seksjonRepository)
    val ident = "12345678910"
    val søknadId = UUID.randomUUID()

    @Test
    fun `Behovløser publiserer løsning på behov Permittert med verdi og gjelderFra`() {
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
        behovløser.løs(lagBehovmelding(ident, søknadId, Permittert))

        testRapid.inspektør.message(0)["@løsning"]["Permittert"].also { løsning ->
            løsning["verdi"].asBoolean() shouldBe true
            løsning["gjelderFra"].asLocalDate() shouldBe søknadstidspunkt.toLocalDate()
        }
    }

    @Test
    fun `Behovløser publiserer løsning på behov Permittert med verdi og gjelderFra fra seksjonsdata`() {
        every {
            seksjonRepository.hentSeksjonsvarEllerKastException(
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
                        "hvordan-har-dette-arbeidsforholdet-endret-seg": "jeg-er-permitert"
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
        behovløser.løs(lagBehovmelding(ident, søknadId, Permittert))

        verify { seksjonRepository.hentSeksjonsvarEllerKastException(ident, søknadId, "arbeidsforhold") }
        verify { søknadRepository.hent(søknadId) }
        testRapid.inspektør.message(0)["@løsning"]["Permittert"].also { løsning ->
            løsning["verdi"].asBoolean() shouldBe true
            løsning["gjelderFra"].asLocalDate() shouldBe søknadstidspunkt.toLocalDate()
        }
    }

    @Suppress("ktlint:standard:max-line-length")
    @Test
    fun `Behovløser publiserer løsning på behov Permittert med verdi og gjelderFra fra seksjonsdata hvor sluttårsak var annet en permittert`() {
        every {
            seksjonRepository.hentSeksjonsvarEllerKastException(
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
        behovløser.løs(lagBehovmelding(ident, søknadId, Permittert))

        verify { seksjonRepository.hentSeksjonsvarEllerKastException(ident, søknadId, "arbeidsforhold") }
        verify { søknadRepository.hent(søknadId) }
        testRapid.inspektør.message(0)["@løsning"]["Permittert"].also { løsning ->
            løsning["verdi"].asBoolean() shouldBe false
            løsning["gjelderFra"].asLocalDate() shouldBe søknadstidspunkt.toLocalDate()
        }
    }

    @Test
    fun `Behovløser publiserer løsning på behov Permittert med verdi og gjelderFra fra seksjonsdata uten arbeidsforhold gir false`() {
        every {
            seksjonRepository.hentSeksjonsvarEllerKastException(
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
        behovløser.løs(lagBehovmelding(ident, søknadId, Permittert))

        verify { seksjonRepository.hentSeksjonsvarEllerKastException(ident, søknadId, "arbeidsforhold") }
        verify { søknadRepository.hent(søknadId) }
        testRapid.inspektør.message(0)["@løsning"]["Permittert"].also { løsning ->
            løsning["verdi"].asBoolean() shouldBe false
            løsning["gjelderFra"].asLocalDate() shouldBe søknadstidspunkt.toLocalDate()
        }
    }

    @Test
    fun `Behovløser setter løsning til true når minst ett arbeidsforhold har sluttårsak PERMITTERT`() {
        val svarMedEttPermittertArbeidsforhold =
            listOf(
                ArbeidsforholdSvar(
                    navn = "arbeidsforhold1",
                    land = "NOR",
                    sluttårsak = Sluttårsak.PERMITTERT,
                ),
                ArbeidsforholdSvar(
                    navn = "arbeidsforhold2",
                    land = "NOR",
                    sluttårsak = Sluttårsak.KONTRAKT_UTGAATT,
                ),
            )

        opplysningRepository.lagre(opplysning(svar = svarMedEttPermittertArbeidsforhold))
        behovløser.rettTilDagpengerUnderPermittering(ident, søknadId) shouldBe true
    }

    @Test
    fun `Behovløser setter løsning til false når ingen arbeidsforhold har sluttårsak PERMITTERT`() {
        val svarUtenPermittertArbeidsforhold =
            listOf(
                ArbeidsforholdSvar(
                    navn = "arbeidsforhold",
                    land = "NOR",
                    sluttårsak = Sluttårsak.ARBEIDSGIVER_KONKURS,
                ),
            )

        opplysningRepository.lagre(opplysning(svar = svarUtenPermittertArbeidsforhold))
        behovløser.rettTilDagpengerUnderPermittering(ident, søknadId) shouldBe false
    }

    @Test
    fun `Behovløser setter løsning til false når det ikke er noen opplysning om arbeidsforhold`() {
        every {
            seksjonRepository.hentSeksjonsvarEllerKastException(
                any(),
                any(),
                any(),
            )
        } throws IllegalStateException("Fant ingen seksjonsvar på arbeidsforhold for søknad=$søknadId")
        behovløser.rettTilDagpengerUnderPermittering(ident, søknadId) shouldBe false
    }

    private fun opplysning(
        svar: List<ArbeidsforholdSvar> =
            listOf(
                ArbeidsforholdSvar(
                    navn = "arbeidsforhold",
                    land = "NOR",
                    sluttårsak = Sluttårsak.PERMITTERT,
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
