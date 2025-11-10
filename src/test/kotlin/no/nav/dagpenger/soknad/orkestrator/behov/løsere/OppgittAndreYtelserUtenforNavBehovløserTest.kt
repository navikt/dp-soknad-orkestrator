package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.OppgittAndreYtelserUtenforNav
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.QuizOpplysning
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Boolsk
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Tekst
import no.nav.dagpenger.soknad.orkestrator.søknad.Søknad
import no.nav.dagpenger.soknad.orkestrator.søknad.Tilstand
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonRepository
import no.nav.dagpenger.soknad.orkestrator.utils.InMemoryQuizOpplysningRepository
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.test.Test

class OppgittAndreYtelserUtenforNavBehovløserTest {
    val opplysningRepository = InMemoryQuizOpplysningRepository()
    val søknadRepository = mockk<SøknadRepository>(relaxed = true)
    val seksjonRepository = mockk<SeksjonRepository>(relaxed = true)
    val testRapid = TestRapid()
    val behovløser = OppgittAndreYtelserUtenforNavBehovløser(testRapid, opplysningRepository, søknadRepository, seksjonRepository)
    val ident = "12345678910"
    val søknadId = UUID.randomUUID()

    @Test
    fun `Behovløser publiserer løsning på behov OppgittAndreYtelserUtenforNav med verdi og gjelderFra`() {
        val opplysning =
            QuizOpplysning(
                beskrivendeId = behovløser.beskrivendeId,
                type = Boolsk,
                svar = false,
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
        behovløser.løs(
            lagBehovmelding(
                ident,
                søknadId,
                OppgittAndreYtelserUtenforNav,
            ),
        )

        testRapid.inspektør.message(0)["@løsning"]["OppgittAndreYtelserUtenforNav"].also { løsning ->
            løsning["verdi"].asBoolean() shouldBe false
            løsning["gjelderFra"].asLocalDate() shouldBe søknadstidspunkt.toLocalDate()
        }
    }

    @Test
    fun `Behovløser publiserer løsning på behov OppgittAndreYtelserUtenforNav med verdi og gjelderFra fra seksjonsdata`() {
        every {
            seksjonRepository.hentSeksjonsvarEllerKastException(
                any(),
                any(),
                any(),
            )
        } returns
            """
            {
              "seksjon": {
                "mottar-du-eller-har-du-søkt-om-pengestøtte-fra-andre-enn-nav": "ja"
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

        behovløser.løs(
            lagBehovmelding(
                ident,
                søknadId,
                OppgittAndreYtelserUtenforNav,
            ),
        )

        verify { seksjonRepository.hentSeksjonsvarEllerKastException(ident, søknadId, "annen-pengestotte") }
        verify { søknadRepository.hent(søknadId) }

        testRapid.inspektør.message(0)["@løsning"]["OppgittAndreYtelserUtenforNav"].also { løsning ->
            løsning["verdi"].asBoolean() shouldBe true
            løsning["gjelderFra"].asLocalDate() shouldBe søknadstidspunkt.toLocalDate()
        }
    }

    @Test
    fun `Behovløser kaster feil dersom det ikke finnes en opplysning som kan besvare behovet`() {
        shouldThrow<IllegalStateException> {
            behovløser.løs(
                lagBehovmelding(
                    ident,
                    søknadId,
                    OppgittAndreYtelserUtenforNav,
                ),
            )
        }
    }
}
