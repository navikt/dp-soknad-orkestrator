package no.nav.dagpenger.soknad.orkestrator.behov

import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.EøsArbeid
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.HelseTilAlleTyperJobb
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.JobbetUtenforNorge
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.KanJobbeDeltid
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.KanJobbeHvorSomHelst
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.Lønnsgaranti
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.Ordinær
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.Permittert
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.PermittertFiskeforedling
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.Søknadsdato
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.Søknadstidspunkt
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.TarUtdanningEllerOpplæring
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.Verneplikt
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.VilligTilÅBytteYrke
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.ØnskerDagpengerFraDato
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.EøsArbeidBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.HelseTilAlleTyperJobbBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.JobbetUtenforNorgeBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.KanJobbeDeltidBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.KanJobbeHvorSomHelstBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.LønnsgarantiBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.OrdinærBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.PermittertBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.PermittertFiskeforedlingBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.SøknadsdatoBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.SøknadstidspunktBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.UtdanningEllerOpplæringBehovløser
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
            ØnskerDagpengerFraDato to ØnskerDagpengerFraDatoBehovløser(rapidsConnection, opplysningRepository),
            EøsArbeid to EøsArbeidBehovløser(rapidsConnection, opplysningRepository),
            KanJobbeDeltid to KanJobbeDeltidBehovløser(rapidsConnection, opplysningRepository),
            HelseTilAlleTyperJobb to HelseTilAlleTyperJobbBehovløser(rapidsConnection, opplysningRepository),
            KanJobbeHvorSomHelst to KanJobbeHvorSomHelstBehovløser(rapidsConnection, opplysningRepository),
            VilligTilÅBytteYrke to VilligTilÅBytteYrkeBehovløser(rapidsConnection, opplysningRepository),
            Søknadstidspunkt to SøknadstidspunktBehovløser(rapidsConnection, opplysningRepository),
            JobbetUtenforNorge to JobbetUtenforNorgeBehovløser(rapidsConnection, opplysningRepository),
            Verneplikt to VernepliktBehovløser(rapidsConnection, opplysningRepository),
            Lønnsgaranti to LønnsgarantiBehovløser(rapidsConnection, opplysningRepository),
            Permittert to PermittertBehovløser(rapidsConnection, opplysningRepository),
            PermittertFiskeforedling to PermittertFiskeforedlingBehovløser(rapidsConnection, opplysningRepository),
            Ordinær to OrdinærBehovløser(rapidsConnection, opplysningRepository),
            Søknadsdato to SøknadsdatoBehovløser(rapidsConnection, opplysningRepository),
            TarUtdanningEllerOpplæring to UtdanningEllerOpplæringBehovløser(rapidsConnection, opplysningRepository),
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
        Lønnsgaranti,
        Permittert,
        PermittertFiskeforedling,
        Ordinær,
        Søknadsdato,
        TarUtdanningEllerOpplæring,
    }
}
