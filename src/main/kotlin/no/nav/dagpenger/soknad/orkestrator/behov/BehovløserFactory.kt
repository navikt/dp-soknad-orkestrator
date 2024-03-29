package no.nav.dagpenger.soknad.orkestrator.behov

import no.nav.dagpenger.soknad.orkestrator.behov.løsere.EøsArbeidBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.HelseTilAlleTyperJobbBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.JobbetUtenforNorgeBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.KanJobbeDeltidBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.KanJobbeHvorSomHelstBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.SøknadstidspunktBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.VilligTilÅBytteYrkeBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.ØnskerDagpengerFraDatoBehovløser
import no.nav.dagpenger.soknad.orkestrator.opplysning.db.OpplysningRepositoryPostgres
import no.nav.helse.rapids_rivers.RapidsConnection

class BehovløserFactory(
    private val rapidsConnection: RapidsConnection,
    private val opplysningRepository: OpplysningRepositoryPostgres,
) {
    fun behovsløser(behov: String): Behovsløser {
        return when (behov) {
            "ØnskerDagpengerFraDato" ->
                ØnskerDagpengerFraDatoBehovløser(
                    rapidsConnection,
                    opplysningRepository,
                )

            "EøsArbeid" ->
                EøsArbeidBehovløser(
                    rapidsConnection,
                    opplysningRepository,
                )

            "KanJobbeDeltid" ->
                KanJobbeDeltidBehovløser(
                    rapidsConnection,
                    opplysningRepository,
                )

            "HelseTilAlleTyperJobb" ->
                HelseTilAlleTyperJobbBehovløser(
                    rapidsConnection,
                    opplysningRepository,
                )

            "KanJobbeHvorSomHelst" ->
                KanJobbeHvorSomHelstBehovløser(
                    rapidsConnection,
                    opplysningRepository,
                )

            "VilligTilÅBytteYrke" ->
                VilligTilÅBytteYrkeBehovløser(
                    rapidsConnection,
                    opplysningRepository,
                )

            "Søknadstidspunkt" ->
                SøknadstidspunktBehovløser(
                    rapidsConnection,
                    opplysningRepository,
                )

            "JobbetUtenforNorge" ->
                JobbetUtenforNorgeBehovløser(
                    rapidsConnection,
                    opplysningRepository,
                )

            else -> throw IllegalArgumentException("Kan ikke løse behov: $behov")
        }
    }
}
