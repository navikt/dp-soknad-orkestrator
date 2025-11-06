package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import no.nav.dagpenger.soknad.orkestrator.behov.Behovløser
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory
import no.nav.dagpenger.soknad.orkestrator.db.Postgres.dataSource
import no.nav.dagpenger.soknad.orkestrator.db.Postgres.withMigratedDb
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.QuizOpplysning
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Boolsk
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Tekst
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.Søknad
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonRepository
import no.nav.dagpenger.soknad.orkestrator.utils.InMemoryQuizOpplysningRepository
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test

class VernepliktBehovløserTest {
    private lateinit var seksjonRepository: SeksjonRepository
    private lateinit var søknadRepository: SøknadRepository
    lateinit var behovløser: Behovløser

    val opplysningRepository = InMemoryQuizOpplysningRepository()
    val testRapid = TestRapid()
    val ident = "12345678910"
    val søknadId = UUID.randomUUID()

    @BeforeTest
    fun setup() {
        withMigratedDb {
            søknadRepository = SøknadRepository(dataSource, mockk<QuizOpplysningRepository>(relaxed = true))
            seksjonRepository =
                SeksjonRepository(
                    dataSource,
                    søknadRepository,
                )
        }
        behovløser = VernepliktBehovløser(testRapid, opplysningRepository, søknadRepository, seksjonRepository)
    }

    @Test
    fun `Behovløser publiserer løsning på behov Verneplikt med verdi og gjelderFra`() {
        val opplysning =
            QuizOpplysning(
                beskrivendeId = behovløser.beskrivendeId,
                type = Boolsk,
                svar = true,
                ident = ident,
                søknadId = søknadId,
            )

        // Må også lagre søknadstidspunkt fordi det er denne som brukes for å sette gjelderFra i første omgang
        // github.com/navikt/dp-soknad/blob/53c7a36d199e207dca01ede6d1b0ceebc18debff/mediator/src/main/kotlin/no/nav/dagpenger/soknad/livssyklus/ferdigstilling/FerdigstillingRoute.kt#L28
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
        behovløser.løs(lagBehovmelding(ident, søknadId, BehovløserFactory.Behov.Verneplikt))

        testRapid.inspektør.message(0)["@løsning"]["Verneplikt"].also { løsning ->
            løsning["verdi"].asBoolean() shouldBe true
            løsning["gjelderFra"].asLocalDate() shouldBe søknadstidspunkt.toLocalDate()
        }
    }

    @Test
    fun `Behovløser publiserer løsning på behov Verneplikt med verdi og gjelderFra fra seksjonRepository`() {
        // Må også lagre søknadstidspunkt fordi det er denne som brukes for å sette gjelderFra i første omgang
        val søknadstidspunkt = ZonedDateTime.now()
        println("søknadid: $søknadId")

        seksjonRepository.søknadRepository.lagre(
            Søknad(
                søknadId = søknadId,
                ident = ident,
                innsendtTidspunkt = søknadstidspunkt.toLocalDateTime(),
            ),
        )
        seksjonRepository.søknadRepository.markerSøknadSomInnsendt(
            søknadId = søknadId,
            innsendtTidspunkt = søknadstidspunkt.toLocalDateTime(),
        )
        seksjonRepository.lagre(
            ident,
            søknadId,
            seksjonId = "verneplikt",
            seksjonsvar = """{"seksjon":{"avtjent-verneplikt":"ja"},"versjon":1}""",
            pdfGrunnlag = """{"pdfGrunnlagKey": "pdfGrunnlagValue"}""",
        )

        behovløser.løs(lagBehovmelding(ident, søknadId, BehovløserFactory.Behov.Verneplikt))

        testRapid.inspektør.message(0)["@løsning"]["Verneplikt"].also { løsning ->
            løsning["verdi"].asBoolean() shouldBe true
            løsning["gjelderFra"].asLocalDate() shouldBe søknadstidspunkt.toLocalDate()
        }
    }

    @Test
    fun `Behovløser kaster feil dersom det ikke finnes en opplysning som kan besvare behovet`() {
        shouldThrow<IllegalStateException> { behovløser.løs(lagBehovmelding(ident, søknadId, BehovløserFactory.Behov.Verneplikt)) }
    }
}
