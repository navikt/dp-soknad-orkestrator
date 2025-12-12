package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.ØnskerDagpengerFraDato
import no.nav.dagpenger.soknad.orkestrator.behov.FellesBehovløserLøsninger
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.QuizOpplysning
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Dato
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Tekst
import no.nav.dagpenger.soknad.orkestrator.søknad.Søknad
import no.nav.dagpenger.soknad.orkestrator.søknad.Tilstand
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonRepository
import no.nav.dagpenger.soknad.orkestrator.utils.InMemoryQuizOpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.utils.januar
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.test.Test

class ØnskerDagpengerFraDatoBehovløserTest {
    val opplysningRepository = InMemoryQuizOpplysningRepository()
    val søknadRepository = mockk<SøknadRepository>(relaxed = true)
    val seksjonRepository = mockk<SeksjonRepository>(relaxed = true)
    val fellesBehovLøserLøsninger =
        FellesBehovløserLøsninger(opplysningRepository, søknadRepository, seksjonRepository)
    val testRapid = TestRapid()
    val behovløser =
        ØnskerDagpengerFraDatoBehovløser(testRapid, opplysningRepository, søknadRepository, seksjonRepository, fellesBehovLøserLøsninger)
    val ident = "12345678910"
    val søknadId = UUID.randomUUID()

    @Test
    fun `Behovløser publiserer løsning på behov ØnskerDagpengerFraDato med verdi og gjelderFra hvis søknadsdato finnes`() {
        val svar = 1.januar(2021)

        val opplysning =
            QuizOpplysning(
                beskrivendeId = "faktum.dagpenger-soknadsdato",
                type = Dato,
                svar = svar,
                ident = ident,
                søknadId = søknadId,
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

        opplysningRepository.lagre(opplysning)
        opplysningRepository.lagre(søknadstidpsunktOpplysning)
        behovløser.løs(lagBehovmelding(ident, søknadId, ØnskerDagpengerFraDato))

        testRapid.inspektør.message(0)["@løsning"]["ØnskerDagpengerFraDato"].also { løsning ->
            løsning["verdi"].asLocalDate() shouldBe svar
            løsning["gjelderFra"].asLocalDate() shouldBe søknadstidspunkt.toLocalDate()
        }
    }

    @Test
    @Suppress("ktlint:standard:max-line-length")
    fun `Behovløser publiserer løsning på behov ØnskerDagpengerFraDato med verdi og gjelderFra hvis søknadsdato ikke finnes, men vi har genopptaksdato`() {
        val svar = 1.januar(2021)

        val opplysning =
            QuizOpplysning(
                beskrivendeId = "faktum.dagpenger-soknadsdato",
                type = Dato,
                svar = svar,
                ident = ident,
                søknadId = søknadId,
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

        opplysningRepository.lagre(opplysning)
        opplysningRepository.lagre(søknadstidpsunktOpplysning)
        behovløser.løs(lagBehovmelding(ident, søknadId, ØnskerDagpengerFraDato))

        testRapid.inspektør.message(0)["@løsning"]["ØnskerDagpengerFraDato"].also { løsning ->
            løsning["verdi"].asLocalDate() shouldBe svar
            løsning["gjelderFra"].asLocalDate() shouldBe søknadstidspunkt.toLocalDate()
        }
    }

    @Test
    fun `Publiser løsning med verdi fra ønskerDagpengerFraDato i seksjonsdata`() {
        every {
            seksjonRepository.hentSeksjonsvarEllerKastException(
                any(),
                any(),
                any(),
            )
        } returns
            """
            {
              "seksjonId": "din-situasjon",
              "seksjonsvar": {
                "harDuMottattDagpengerFraNavILøpetAvDeSiste52Ukene": "nei",
                "hvilkenDatoSøkerDuDagpengerFra": "2025-11-24"
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

        behovløser.løs(lagBehovmelding(ident, søknadId, ØnskerDagpengerFraDato))

        verify { seksjonRepository.hentSeksjonsvarEllerKastException(ident, søknadId, "din-situasjon") }
        verify { søknadRepository.hent(søknadId) }
        testRapid.inspektør.message(0)["@løsning"]["ØnskerDagpengerFraDato"].also { løsning ->
            løsning["verdi"].asLocalDate() shouldBe LocalDate.of(2025, 11, 24)
            løsning["gjelderFra"].asLocalDate() shouldBe søknadstidspunkt.toLocalDate()
        }
    }

    @Test
    fun `Publiser løsning med verdi fra gjenopptaksdato i seksjonsdata`() {
        every {
            seksjonRepository.hentSeksjonsvarEllerKastException(
                any(),
                any(),
                any(),
            )
        } returns
            """
            {
              "seksjonId": "din-situasjon",
              "seksjonsvar": {
                "harDuMottattDagpengerFraNavILøpetAvDeSiste52Ukene": "ja",
                "årsakTilAtDagpengeneBleStanset": "Jeg jobbet litt",
                "hvilkenDatoSøkerDuGjenopptakFra": "2024-10-15"
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

        behovløser.løs(lagBehovmelding(ident, søknadId, ØnskerDagpengerFraDato))

        verify { seksjonRepository.hentSeksjonsvarEllerKastException(ident, søknadId, "din-situasjon") }
        verify { søknadRepository.hent(søknadId) }
        testRapid.inspektør.message(0)["@løsning"]["ØnskerDagpengerFraDato"].also { løsning ->
            løsning["verdi"].asLocalDate() shouldBe LocalDate.of(2024, 10, 15)
            løsning["gjelderFra"].asLocalDate() shouldBe søknadstidspunkt.toLocalDate()
        }
    }

    @Test
    fun `Behovløser kaster feil dersom det ikke finnes en opplysning som kan besvare behovet`() {
        shouldThrow<IllegalStateException> {
            behovløser.løs(
                lagBehovmelding(ident, søknadId, ØnskerDagpengerFraDato),
            )
        }
    }
}
