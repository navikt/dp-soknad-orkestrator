package no.nav.dagpenger.soknad.orkestrator.behov

import io.kotest.matchers.shouldBe
import io.mockk.mockk
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.EøsArbeidBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.HelseTilAlleTyperJobbBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.JobbetUtenforNorgeBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.KanJobbeDeltidBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.KanJobbeHvorSomHelstBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.LønnsgarantiBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.PermittertFiskeforedlingBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.SøknadstidspunktBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.VernepliktBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.VilligTilÅBytteYrkeBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.ØnskerDagpengerFraDatoBehovløser
import no.nav.dagpenger.soknad.orkestrator.opplysning.db.OpplysningRepositoryPostgres
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import kotlin.test.Test

class BehovløserFactoryTest {
    private val testRapid = TestRapid()
    private val opplysningRepository = mockk<OpplysningRepositoryPostgres>(relaxed = true)
    private val behovløserFactory = BehovløserFactory(testRapid, opplysningRepository)

    @Test
    fun `Skal returnere riktig behovløser basert på gitt behov`() {
        behovløserFactory.behovløserFor(BehovløserFactory.Behov.ØnskerDagpengerFraDato) is ØnskerDagpengerFraDatoBehovløser
        behovløserFactory.behovløserFor(BehovløserFactory.Behov.EøsArbeid) is EøsArbeidBehovløser
        behovløserFactory.behovløserFor(BehovløserFactory.Behov.KanJobbeDeltid) is KanJobbeDeltidBehovløser
        behovløserFactory.behovløserFor(BehovløserFactory.Behov.HelseTilAlleTyperJobb) is HelseTilAlleTyperJobbBehovløser
        behovløserFactory.behovløserFor(BehovløserFactory.Behov.KanJobbeHvorSomHelst) is KanJobbeHvorSomHelstBehovløser
        behovløserFactory.behovløserFor(BehovløserFactory.Behov.VilligTilÅBytteYrke) is VilligTilÅBytteYrkeBehovløser
        behovløserFactory.behovløserFor(BehovløserFactory.Behov.Søknadstidspunkt) is SøknadstidspunktBehovløser
        behovløserFactory.behovløserFor(BehovløserFactory.Behov.JobbetUtenforNorge) is JobbetUtenforNorgeBehovløser
        behovløserFactory.behovløserFor(BehovløserFactory.Behov.Verneplikt) is VernepliktBehovløser
        behovløserFactory.behovløserFor(BehovløserFactory.Behov.Lønnsgaranti) is LønnsgarantiBehovløser
        behovløserFactory.behovløserFor(BehovløserFactory.Behov.PermittertFiskeforedling) is PermittertFiskeforedlingBehovløser
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
                "PermittertFiskeforedling",
            )
    }
}
