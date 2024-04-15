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
    private var behovløsere: Map<String, Behovløser> =
        mapOf(
            "ØnskerDagpengerFraDato" to ØnskerDagpengerFraDatoBehovløser(rapidsConnection, opplysningRepository),
            "EøsArbeid" to EøsArbeidBehovløser(rapidsConnection, opplysningRepository),
            "KanJobbeDeltid" to KanJobbeDeltidBehovløser(rapidsConnection, opplysningRepository),
            "HelseTilAlleTyperJobb" to HelseTilAlleTyperJobbBehovløser(rapidsConnection, opplysningRepository),
            "KanJobbeHvorSomHelst" to KanJobbeHvorSomHelstBehovløser(rapidsConnection, opplysningRepository),
            "VilligTilÅBytteYrke" to VilligTilÅBytteYrkeBehovløser(rapidsConnection, opplysningRepository),
            "Søknadstidspunkt" to SøknadstidspunktBehovløser(rapidsConnection, opplysningRepository),
            "JobbetUtenforNorge" to JobbetUtenforNorgeBehovløser(rapidsConnection, opplysningRepository),
            "Verneplikt" to VernepliktBehovløser(rapidsConnection, opplysningRepository),
        )

    fun behovløserFor(behov: String): Behovløser {
        return behovløsere[behov] ?: throw IllegalArgumentException("Fant ikke behovløser for behov: $behov")
    }

    fun behov() = behovløsere.keys.toList()
}
