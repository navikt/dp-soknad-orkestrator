package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.QuizOpplysning
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Tekst
import no.nav.dagpenger.soknad.orkestrator.søknad.Søknad
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.utils.InMemoryQuizOpplysningRepository
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.test.Test

class SøknadsdatoBehovløserTest {
    val opplysningRepository = InMemoryQuizOpplysningRepository()
    val søknadRepository = mockk<SøknadRepository>(relaxed = true)
    val testRapid = TestRapid()
    val behovløser = SøknadsdatoBehovløser(testRapid, opplysningRepository, søknadRepository)
    val ident = "12345678910"
    val søknadId = UUID.randomUUID()

    @Test
    fun `Behovløser publiserer løsning på behov Søknadsdato med verdi og gjelderFra`() {
        val svar = ZonedDateTime.now()
        val opplysning =
            QuizOpplysning(
                beskrivendeId = behovløser.beskrivendeId,
                type = Tekst,
                svar = svar.toString(),
                ident = ident,
                søknadId = søknadId,
            )

        opplysningRepository.lagre(opplysning)
        behovløser.løs(lagBehovmelding(ident, søknadId, BehovløserFactory.Behov.Søknadsdato))

        testRapid.inspektør.message(0)["@løsning"]["Søknadsdato"].also { løsning ->
            løsning["verdi"].asLocalDate() shouldBe svar.toLocalDate()
            løsning["gjelderFra"].asLocalDate() shouldBe svar.toLocalDate()
        }
    }

    @Test
    fun `Behovløser publiserer løsning på behov Søknadsdato med verdi og gjelderFra fra søknadstabellen`() {
        val svar = LocalDateTime.now()
        every { søknadRepository.hent(any()) } returns
            Søknad(
                ident = ident,
                søknadId = søknadId,
                innsendtTidspunkt = svar,
            )

        behovløser.løs(lagBehovmelding(ident, søknadId, BehovløserFactory.Behov.Søknadsdato))

        verify { søknadRepository.hent(søknadId) }
        testRapid.inspektør.message(0)["@løsning"]["Søknadsdato"].also { løsning ->
            løsning["verdi"].asLocalDate() shouldBe svar.toLocalDate()
            løsning["gjelderFra"].asLocalDate() shouldBe svar.toLocalDate()
        }
    }

    @Test
    fun `Behovløser kaster feil dersom det ikke finnes en opplysning som kan besvare behovet`() {
        every { søknadRepository.hent(søknadId) } returns null

        shouldThrow<IllegalStateException> { behovløser.løs(lagBehovmelding(ident, søknadId, BehovløserFactory.Behov.Søknadsdato)) }
    }
}
