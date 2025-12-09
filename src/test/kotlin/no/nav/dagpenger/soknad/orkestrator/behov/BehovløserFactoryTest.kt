package no.nav.dagpenger.soknad.orkestrator.behov

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import no.nav.dagpenger.soknad.orkestrator.db.Postgres.dataSource
import no.nav.dagpenger.soknad.orkestrator.db.Postgres.withMigratedDb
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepositoryPostgres
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonRepository
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.BeforeTest
import kotlin.test.Test

class BehovløserFactoryTest {
    private lateinit var seksjonRepository: SeksjonRepository
    private lateinit var søknadRepository: SøknadRepository
    private val testRapid = TestRapid()
    private val opplysningRepository = mockk<QuizOpplysningRepositoryPostgres>(relaxed = true)
    private lateinit var behovløserFactory: BehovløserFactory

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
        behovløserFactory = BehovløserFactory(testRapid, opplysningRepository, seksjonRepository, søknadRepository)
    }

    companion object {
        @JvmStatic
        fun behovProvider() = BehovløserFactory.Behov.entries.map { arrayOf(it) }
    }

    @ParameterizedTest
    @MethodSource("behovProvider")
    fun `Skal returnere riktig behovløser basert på gitt behov`(behov: BehovløserFactory.Behov) {
        behovløserFactory.behovløserFor(behov).behov shouldBe behov.name
    }

    @Test
    fun `Kan hente ut alle behov`() {
        behovløserFactory.behov().shouldContainExactlyInAnyOrder(
            "OppgittAndreYtelserUtenforNav",
            "ØnskerDagpengerFraDato",
            "EØSArbeid",
            "KanJobbeDeltid",
            "HelseTilAlleTyperJobb",
            "KanJobbeHvorSomHelst",
            "VilligTilÅBytteYrke",
            "JobbetUtenforNorge",
            "Verneplikt",
            "Lønnsgaranti",
            "Permittert",
            "PermittertFiskeforedling",
            "Ordinær",
            "Søknadsdato",
            "TarUtdanningEllerOpplæring",
            "Barnetillegg",
            "BarnetilleggV2",
            "AndreØkonomiskeYtelser",
            "ØnsketArbeidstid",
            "HarTilleggsopplysninger",
            "BostedslandErNorge",
            "PermittertGrensearbeider",
            "EgetGårdsbruk",
            "EgenNæringsvirksomhet",
            "SøknadsdataSTSB",
        )
    }
}
