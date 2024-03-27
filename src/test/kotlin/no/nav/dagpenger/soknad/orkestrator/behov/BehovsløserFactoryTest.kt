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
        behovløserFactory.behovsløser("ØnskerDagpengerFraDato") is ØnskerDagpengerFraDatoBehovløser
        behovløserFactory.behovsløser("EøsArbeid") is EøsArbeidBehovløser
        behovløserFactory.behovsløser("KanJobbeDeltid") is KanJobbeDeltidBehovløser
        behovløserFactory.behovsløser("HelseTilAlleTyperJobb") is HelseTilAlleTyperJobbBehovløser
        behovløserFactory.behovsløser("KanJobbeHvorSomHelst") is KanJobbeHvorSomHelstBehovløser
        behovløserFactory.behovsløser("VilligTilÅBytteYrke") is VilligTilÅBytteYrkeBehovløser
        behovløserFactory.behovsløser("Søknadstidspunkt") is SøknadstidspunktBehovløser
        behovløserFactory.behovsløser("JobbetUtenforNorge") is JobbetUtenforNorgeBehovløser
    }
}
