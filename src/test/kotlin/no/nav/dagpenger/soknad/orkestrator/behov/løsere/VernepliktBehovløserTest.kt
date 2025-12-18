package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory
import no.nav.dagpenger.soknad.orkestrator.behov.FellesBehovløserLøsninger
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.QuizOpplysning
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Tekst
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonRepository
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.test.Test

class VernepliktBehovløserTest {
    val opplysningRepository = mockk<QuizOpplysningRepository>()
    val testRapid = TestRapid()
    val søknadRepository = mockk<SøknadRepository>(relaxed = true)
    val seksjonRepository = mockk<SeksjonRepository>(relaxed = true)
    val fellesBehovLøserLøsninger = mockk<FellesBehovløserLøsninger>(relaxed = true)
    val behovløser = VernepliktBehovløser(testRapid, opplysningRepository, søknadRepository, seksjonRepository, fellesBehovLøserLøsninger)
    val ident = "12345678910"
    val søknadId = UUID.randomUUID()

    @Test
    fun `Behovløser publiserer løsning på behov Verneplikt med verdi og gjelderFra`() {
        val søknadstidspunkt = ZonedDateTime.now()
        val cases = listOf(true, false)
        cases.forEach { avtjentVerneplikt ->
            every {
                fellesBehovLøserLøsninger.harSøkerenAvtjentVerneplikt(
                    any(),
                    any(),
                    any(),
                    any(),
                )
            } returns avtjentVerneplikt

            every {
                opplysningRepository.hent(any(), any(), any())
            } returns
                QuizOpplysning(
                    beskrivendeId = "søknadstidspunkt",
                    type = Tekst,
                    svar = søknadstidspunkt.toString(),
                    ident = ident,
                    søknadId = søknadId,
                )

            behovløser.løs(lagBehovmelding(ident, søknadId, BehovløserFactory.Behov.Verneplikt))

            verify {
                fellesBehovLøserLøsninger.harSøkerenAvtjentVerneplikt(
                    BehovløserFactory.Behov.Verneplikt.name,
                    behovløser.beskrivendeId,
                    ident,
                    søknadId,
                )
            }

            verify {
                opplysningRepository.hent(
                    "søknadstidspunkt",
                    ident,
                    søknadId,
                )
            }

            testRapid.inspektør.message(0)["@løsning"]["Verneplikt"].also { løsning ->
                løsning["verdi"].asBoolean() shouldBe avtjentVerneplikt
                løsning["gjelderFra"].asLocalDate() shouldBe LocalDate.now()
            }
            testRapid.reset()
        }
    }

    @Test
    fun `Behovløser kaster feil dersom det ikke finnes en opplysning som kan besvare behovet`() {
        val behov = BehovløserFactory.Behov.Verneplikt.name
        every {
            fellesBehovLøserLøsninger.harSøkerenAvtjentVerneplikt(
                any(),
                any(),
                any(),
                any(),
            )
        } throws IllegalStateException("Fant ingen opplysning på behov $behov for søknad med id: $søknadId")

        shouldThrow<IllegalStateException> { behovløser.løs(lagBehovmelding(ident, søknadId, BehovløserFactory.Behov.Verneplikt)) }

        verify {
            fellesBehovLøserLøsninger.harSøkerenAvtjentVerneplikt(
                behov,
                behovløser.beskrivendeId,
                ident,
                søknadId,
            )
        }
    }
}
