package no.nav.dagpenger.soknad.orkestrator.behov

import io.mockk.mockk
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.EøsArbeidBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.HelseTilAlleTyperJobbBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.JobbetUtenforNorgeBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.KanJobbeDeltidBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.KanJobbeHvorSomHelstBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.SøknadstidspunktBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.VilligTilÅBytteYrkeBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.ØnskerDagpengerFraDatoBehovløser
import kotlin.test.Test

class BehovsløserFactoryTest {
    private val behovløserFactory = mockk<BehovløserFactory>(relaxed = true)

    @Test
    fun `skal returnere riktig behovløser basert på gitt behov`() {
        behovløserFactory.behovløserFor("ØnskerDagpengerFraDato") is ØnskerDagpengerFraDatoBehovløser
        behovløserFactory.behovløserFor("EøsArbeid") is EøsArbeidBehovløser
        behovløserFactory.behovløserFor("KanJobbeDeltid") is KanJobbeDeltidBehovløser
        behovløserFactory.behovløserFor("HelseTilAlleTyperJobb") is HelseTilAlleTyperJobbBehovløser
        behovløserFactory.behovløserFor("KanJobbeHvorSomHelst") is KanJobbeHvorSomHelstBehovløser
        behovløserFactory.behovløserFor("VilligTilÅBytteYrke") is VilligTilÅBytteYrkeBehovløser
        behovløserFactory.behovløserFor("Søknadstidspunkt") is SøknadstidspunktBehovløser
        behovløserFactory.behovløserFor("JobbetUtenforNorge") is JobbetUtenforNorgeBehovløser
    }
}
