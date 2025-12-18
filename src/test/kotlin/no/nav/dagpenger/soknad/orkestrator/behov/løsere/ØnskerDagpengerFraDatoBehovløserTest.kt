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
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Tekst
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonRepository
import no.nav.dagpenger.soknad.orkestrator.utils.januar
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.test.Test

class ØnskerDagpengerFraDatoBehovløserTest {
    val opplysningRepository = mockk<QuizOpplysningRepository>()
    val søknadRepository = mockk<SøknadRepository>(relaxed = true)
    val seksjonRepository = mockk<SeksjonRepository>(relaxed = true)
    val fellesBehovLøserLøsninger = mockk<FellesBehovløserLøsninger>(relaxed = true)
    val testRapid = TestRapid()
    val behovløser =
        ØnskerDagpengerFraDatoBehovløser(testRapid, opplysningRepository, søknadRepository, seksjonRepository, fellesBehovLøserLøsninger)
    val ident = "12345678910"
    val søknadId = UUID.randomUUID()

    @Test
    fun `Behovløser publiserer løsning på behov ØnskerDagpengerFraDato med verdi og gjelderFra hvis søknadsdato finnes`() {
        val svar = 1.januar(2021)

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
        every {
            fellesBehovLøserLøsninger.ønskerDagpengerFraDato(
                any(),
                any(),
                any(),
            )
        } returns svar

        every {
            opplysningRepository.hent(
                any(),
                any(),
                any(),
            )
        } returns søknadstidpsunktOpplysning

        behovløser.løs(lagBehovmelding(ident, søknadId, ØnskerDagpengerFraDato))

        verify {
            fellesBehovLøserLøsninger.ønskerDagpengerFraDato(
                ident,
                søknadId,
                ØnskerDagpengerFraDato.name,
            )
        }

        verify {
            opplysningRepository.hent(
                "søknadstidspunkt",
                ident,
                søknadId,
            )
        }

        testRapid.inspektør.message(0)["@løsning"]["ØnskerDagpengerFraDato"].also { løsning ->
            løsning["verdi"].asLocalDate() shouldBe svar
            løsning["gjelderFra"].asLocalDate() shouldBe søknadstidspunkt.toLocalDate()
        }
    }

    @Test
    fun `Behovløser kaster feil dersom det ikke finnes en opplysning som kan besvare behovet`() {
        val cases = listOf("Søknadsdata", "ØnskerDagpengerFraDato")
        cases.forEach { behov ->
            every {
                fellesBehovLøserLøsninger.ønskerDagpengerFraDato(
                    any(),
                    any(),
                    any(),
                )
            } throws
                IllegalStateException(
                    "Fant ingen opplysning på behov $behov for søknad med id: $søknadId",
                )

            shouldThrow<IllegalStateException> {
                behovløser.løs(
                    lagBehovmelding(ident, søknadId, ØnskerDagpengerFraDato),
                )
            }
        }
    }
}
