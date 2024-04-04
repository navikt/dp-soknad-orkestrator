package no.nav.dagpenger.soknad.orkestrator.behov

import io.kotest.matchers.shouldBe
import io.mockk.mockk
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.EøsArbeidBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.HelseTilAlleTyperJobbBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.JobbetUtenforNorgeBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.KanJobbeDeltidBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.KanJobbeHvorSomHelstBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.SøknadstidspunktBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.VilligTilÅBytteYrkeBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.ØnskerDagpengerFraDatoBehovløser
import no.nav.dagpenger.soknad.orkestrator.opplysning.db.OpplysningRepositoryPostgres
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import kotlin.test.Test

class BehovsløserFactoryTest {
    private val testRapid = TestRapid()
    private val opplysningRepository = mockk<OpplysningRepositoryPostgres>(relaxed = true)
    private val behovløserFactory = BehovløserFactory(testRapid, opplysningRepository)

    @Test
    fun `Skal returnere riktig behovløser basert på gitt behov`() {
        behovløserFactory.behovløserFor("ØnskerDagpengerFraDato") is ØnskerDagpengerFraDatoBehovløser
        behovløserFactory.behovløserFor("EøsArbeid") is EøsArbeidBehovløser
        behovløserFactory.behovløserFor("KanJobbeDeltid") is KanJobbeDeltidBehovløser
        behovløserFactory.behovløserFor("HelseTilAlleTyperJobb") is HelseTilAlleTyperJobbBehovløser
        behovløserFactory.behovløserFor("KanJobbeHvorSomHelst") is KanJobbeHvorSomHelstBehovløser
        behovløserFactory.behovløserFor("VilligTilÅBytteYrke") is VilligTilÅBytteYrkeBehovløser
        behovløserFactory.behovløserFor("Søknadstidspunkt") is SøknadstidspunktBehovløser
        behovløserFactory.behovløserFor("JobbetUtenforNorge") is JobbetUtenforNorgeBehovløser
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
            )
    }
}
