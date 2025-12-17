package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
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
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.test.Test

class EØSArbeidBehovløserTest {
    val opplysningRepository = mockk<QuizOpplysningRepository>()
    val testRapid = TestRapid()
    val søknadRepository = mockk<SøknadRepository>(relaxed = true)
    val seksjonRepository = mockk<SeksjonRepository>(relaxed = true)
    val fellesBehovLøserLøsninger = mockk<FellesBehovløserLøsninger>(relaxed = true)

    val behovløser =
        EØSArbeidBehovløser(
            testRapid,
            opplysningRepository,
            søknadRepository,
            seksjonRepository,
            fellesBehovLøserLøsninger,
        )
    val ident = "12345678910"
    val søknadId = UUID.randomUUID()

    @Test
    fun `Behovløser publiserer løsning på behov EøsArbeid med verdi og gjelderFra`() {
        val søknadstidspunkt = ZonedDateTime.now()
        val cases = listOf(true, false)
        cases.forEach { eøsArbeidsforhold ->
            every {
                fellesBehovLøserLøsninger.harSøkerenHattArbeidsforholdIEøs(
                    any(),
                    any(),
                    any(),
                )
            } returns eøsArbeidsforhold

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

            behovløser.løs(lagBehovmelding(ident, søknadId, BehovløserFactory.Behov.EØSArbeid))
            verify {
                fellesBehovLøserLøsninger.harSøkerenHattArbeidsforholdIEøs(
                    "faktum.eos-arbeid-siste-36-mnd",
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

            testRapid.inspektør.message(0)["@løsning"]["EØSArbeid"].also { løsning ->
                løsning["verdi"].asBoolean() shouldBe eøsArbeidsforhold
                løsning["gjelderFra"].asLocalDate() shouldBe søknadstidspunkt.toLocalDate()
            }
            testRapid.reset()
        }
    }
}
