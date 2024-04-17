package no.nav.dagpenger.soknad.orkestrator.behov

import no.nav.dagpenger.soknad.orkestrator.behov.løsere.EøsArbeidBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.HelseTilAlleTyperJobbBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.JobbetUtenforNorgeBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.KanJobbeDeltidBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.KanJobbeHvorSomHelstBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.SøknadstidspunktBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.VernepliktBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.VilligTilÅBytteYrkeBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.ØnskerDagpengerFraDatoBehovløser
import no.nav.dagpenger.soknad.orkestrator.opplysning.db.OpplysningRepositoryPostgres
import no.nav.helse.rapids_rivers.RapidsConnection

class BehovløserFactory(
    rapidsConnection: RapidsConnection,
    opplysningRepository: OpplysningRepositoryPostgres,
) {
    private val behovløsere: Map<Behov, Behovløser> =
        mapOf(
            Behov.ØnskerDagpengerFraDato to ØnskerDagpengerFraDatoBehovløser(rapidsConnection, opplysningRepository),
            Behov.EøsArbeid to EøsArbeidBehovløser(rapidsConnection, opplysningRepository),
            Behov.KanJobbeDeltid to KanJobbeDeltidBehovløser(rapidsConnection, opplysningRepository),
            Behov.HelseTilAlleTyperJobb to HelseTilAlleTyperJobbBehovløser(rapidsConnection, opplysningRepository),
            Behov.KanJobbeHvorSomHelst to KanJobbeHvorSomHelstBehovløser(rapidsConnection, opplysningRepository),
            Behov.VilligTilÅBytteYrke to VilligTilÅBytteYrkeBehovløser(rapidsConnection, opplysningRepository),
            Behov.Søknadstidspunkt to SøknadstidspunktBehovløser(rapidsConnection, opplysningRepository),
            Behov.JobbetUtenforNorge to JobbetUtenforNorgeBehovløser(rapidsConnection, opplysningRepository),
            Behov.Verneplikt to VernepliktBehovløser(rapidsConnection, opplysningRepository),
        )

    fun behovløserFor(behov: Behov): Behovløser {
        return behovløsere[behov] ?: throw IllegalArgumentException("Fant ikke behovløser for behov: $behov")
    }

    fun behov() = behovløsere.keys.map { it.name }.toList()

    enum class Behov {
        ØnskerDagpengerFraDato,
        EøsArbeid,
        KanJobbeDeltid,
        HelseTilAlleTyperJobb,
        KanJobbeHvorSomHelst,
        VilligTilÅBytteYrke,
        Søknadstidspunkt,
        JobbetUtenforNorge,
        Verneplikt,
    }
}
