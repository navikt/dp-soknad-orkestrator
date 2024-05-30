package no.nav.dagpenger.soknad.orkestrator.behov

import io.kotest.matchers.shouldBe
import io.mockk.mockk
import no.nav.dagpenger.soknad.orkestrator.opplysning.db.OpplysningRepositoryPostgres
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.Test

class BehovløserFactoryTest {
    private val testRapid = TestRapid()
    private val opplysningRepository = mockk<OpplysningRepositoryPostgres>(relaxed = true)
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
        behovløserFactory.behov() shouldBe
            listOf(
                "ØnskerDagpengerFraDato",
                "EøsArbeid",
                "KanJobbeDeltid",
                "HelseTilAlleTyperJobb",
                "KanJobbeHvorSomHelst",
                "VilligTilÅBytteYrke",
                "Søknadstidspunkt",
                "JobbetUtenforNorge",
                "Verneplikt",
                "Lønnsgaranti",
                "Permittert",
                "PermittertFiskeforedling",
                "Ordinær",
                "Søknadsdato",
                "TarUtdanningEllerOpplæring",
            )
    }
}
