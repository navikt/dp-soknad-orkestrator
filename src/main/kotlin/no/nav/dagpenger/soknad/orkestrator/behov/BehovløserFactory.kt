package no.nav.dagpenger.soknad.orkestrator.behov

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.AndreØkonomiskeYtelser
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.Barnetillegg
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.BostedslandErNorge
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.EØSArbeid
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.HarTilleggsopplysninger
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.HelseTilAlleTyperJobb
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.JobbetUtenforNorge
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.KanJobbeDeltid
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.KanJobbeHvorSomHelst
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.Lønnsgaranti
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.OppgittAndreYtelserUtenforNav
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.Ordinær
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.Permittert
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.PermittertFiskeforedling
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.PermittertGrensearbeider
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.Søknadsdato
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.TarUtdanningEllerOpplæring
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.Verneplikt
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.VilligTilÅBytteYrke
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.ØnskerDagpengerFraDato
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.ØnsketArbeidstid
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.AndreØkonomiskeYtelserBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.BarnetilleggBehovLøser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.BostedslandErNorgeBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.EØSArbeidBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.HarTilleggsopplysningerBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.HelseTilAlleTyperJobbBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.JobbetUtenforNorgeBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.KanJobbeDeltidBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.KanJobbeHvorSomHelstBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.LønnsgarantiBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.OppgittAndreYtelserUtenforNavBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.OrdinærBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.PermittertBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.PermittertFiskeforedlingBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.PermittertGrensearbeiderBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.SøknadsdatoBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.UtdanningEllerOpplæringBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.VernepliktBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.VilligTilÅBytteYrkeBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.ØnskerDagpengerFraDatoBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.ØnsketArbeidstidBehovløser
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepositoryPostgres

class BehovløserFactory(
    rapidsConnection: RapidsConnection,
    opplysningRepository: QuizOpplysningRepositoryPostgres,
) {
    private val behovløsere: Map<Behov, Behovløser> =
        mapOf(
            OppgittAndreYtelserUtenforNav to OppgittAndreYtelserUtenforNavBehovløser(rapidsConnection, opplysningRepository),
            ØnskerDagpengerFraDato to ØnskerDagpengerFraDatoBehovløser(rapidsConnection, opplysningRepository),
            EØSArbeid to EØSArbeidBehovløser(rapidsConnection, opplysningRepository),
            KanJobbeDeltid to KanJobbeDeltidBehovløser(rapidsConnection, opplysningRepository),
            HelseTilAlleTyperJobb to HelseTilAlleTyperJobbBehovløser(rapidsConnection, opplysningRepository),
            KanJobbeHvorSomHelst to KanJobbeHvorSomHelstBehovløser(rapidsConnection, opplysningRepository),
            VilligTilÅBytteYrke to VilligTilÅBytteYrkeBehovløser(rapidsConnection, opplysningRepository),
            JobbetUtenforNorge to JobbetUtenforNorgeBehovløser(rapidsConnection, opplysningRepository),
            Verneplikt to VernepliktBehovløser(rapidsConnection, opplysningRepository),
            Lønnsgaranti to LønnsgarantiBehovløser(rapidsConnection, opplysningRepository),
            Permittert to PermittertBehovløser(rapidsConnection, opplysningRepository),
            PermittertFiskeforedling to PermittertFiskeforedlingBehovløser(rapidsConnection, opplysningRepository),
            Ordinær to OrdinærBehovløser(rapidsConnection, opplysningRepository),
            Søknadsdato to SøknadsdatoBehovløser(rapidsConnection, opplysningRepository),
            TarUtdanningEllerOpplæring to UtdanningEllerOpplæringBehovløser(rapidsConnection, opplysningRepository),
            Barnetillegg to BarnetilleggBehovLøser(rapidsConnection, opplysningRepository),
            AndreØkonomiskeYtelser to AndreØkonomiskeYtelserBehovløser(rapidsConnection, opplysningRepository),
            ØnsketArbeidstid to ØnsketArbeidstidBehovløser(rapidsConnection, opplysningRepository),
            HarTilleggsopplysninger to HarTilleggsopplysningerBehovløser(rapidsConnection, opplysningRepository),
            BostedslandErNorge to BostedslandErNorgeBehovløser(rapidsConnection, opplysningRepository),
            PermittertGrensearbeider to PermittertGrensearbeiderBehovløser(rapidsConnection, opplysningRepository),
        )

    fun behovløserFor(behov: Behov): Behovløser =
        behovløsere[behov] ?: throw IllegalArgumentException("Fant ikke behovløser for behov: $behov")

    fun behov() = behovløsere.keys.map { it.name }.toList()

    enum class Behov {
        OppgittAndreYtelserUtenforNav,
        ØnskerDagpengerFraDato,
        EØSArbeid,
        KanJobbeDeltid,
        HelseTilAlleTyperJobb,
        KanJobbeHvorSomHelst,
        VilligTilÅBytteYrke,
        JobbetUtenforNorge,
        Verneplikt,
        Lønnsgaranti,
        Permittert,
        PermittertFiskeforedling,
        Ordinær,
        Søknadsdato,
        TarUtdanningEllerOpplæring,
        Barnetillegg,
        AndreØkonomiskeYtelser,
        ØnsketArbeidstid,
        HarTilleggsopplysninger,
        BostedslandErNorge,
        PermittertGrensearbeider,
    }
}
