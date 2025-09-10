package no.nav.dagpenger.soknad.orkestrator.behov

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepositoryPostgres
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.Test

class BehovløserFactoryTest {
    private val testRapid = TestRapid()
    private val opplysningRepository = mockk<QuizOpplysningRepositoryPostgres>(relaxed = true)
    private val behovløserFactory = BehovløserFactory(testRapid, opplysningRepository)

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
        )
    }
}
