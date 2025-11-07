package no.nav.dagpenger.soknad.orkestrator.behov

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.AndreØkonomiskeYtelser
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.Barnetillegg
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.BarnetilleggV2
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.BostedslandErNorge
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.EgenNæringsvirksomhet
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.EgetGårdsbruk
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
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.BarnetilleggV2BehovLøser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.BostedslandErNorgeBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.EgenNæringsvirksomhetBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.EgetGårdsbrukBehovløser
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
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonRepository

class BehovløserFactory(
    rapidsConnection: RapidsConnection,
    opplysningRepository: QuizOpplysningRepositoryPostgres,
    seksjonRepository: SeksjonRepository,
    søknadRepository: SøknadRepository,
) {
    private val behovløsere: Map<Behov, Behovløser> =
        mapOf(
            OppgittAndreYtelserUtenforNav to
                OppgittAndreYtelserUtenforNavBehovløser(
                    rapidsConnection,
                    opplysningRepository,
                    søknadRepository,
                    seksjonRepository,
                ),
            ØnskerDagpengerFraDato to
                ØnskerDagpengerFraDatoBehovløser(
                    rapidsConnection,
                    opplysningRepository,
                    søknadRepository,
                ),
            EØSArbeid to
                EØSArbeidBehovløser(
                    rapidsConnection,
                    opplysningRepository,
                    søknadRepository,
                    seksjonRepository,
                ),
            KanJobbeDeltid to KanJobbeDeltidBehovløser(rapidsConnection, opplysningRepository, søknadRepository, seksjonRepository),
            HelseTilAlleTyperJobb to
                HelseTilAlleTyperJobbBehovløser(
                    rapidsConnection,
                    opplysningRepository,
                    søknadRepository,
                    seksjonRepository,
                ),
            KanJobbeHvorSomHelst to
                KanJobbeHvorSomHelstBehovløser(
                    rapidsConnection,
                    opplysningRepository,
                    søknadRepository,
                    seksjonRepository,
                ),
            VilligTilÅBytteYrke to
                VilligTilÅBytteYrkeBehovløser(
                    rapidsConnection,
                    opplysningRepository,
                    søknadRepository,
                    seksjonRepository,
                ),
            JobbetUtenforNorge to
                JobbetUtenforNorgeBehovløser(
                    rapidsConnection,
                    opplysningRepository,
                    søknadRepository,
                    seksjonRepository,
                ),
            Verneplikt to VernepliktBehovløser(rapidsConnection, opplysningRepository, søknadRepository, seksjonRepository),
            Lønnsgaranti to
                LønnsgarantiBehovløser(
                    rapidsConnection,
                    opplysningRepository,
                    søknadRepository,
                    seksjonRepository,
                ),
            Permittert to
                PermittertBehovløser(
                    rapidsConnection,
                    opplysningRepository,
                    søknadRepository,
                    seksjonRepository,
                ),
            PermittertFiskeforedling to
                PermittertFiskeforedlingBehovløser(
                    rapidsConnection,
                    opplysningRepository,
                    søknadRepository,
                    seksjonRepository,
                ),
            Ordinær to OrdinærBehovløser(rapidsConnection, opplysningRepository, søknadRepository, seksjonRepository),
            Søknadsdato to SøknadsdatoBehovløser(rapidsConnection, opplysningRepository, søknadRepository),
            TarUtdanningEllerOpplæring to
                UtdanningEllerOpplæringBehovløser(rapidsConnection, opplysningRepository, søknadRepository, seksjonRepository),
            Barnetillegg to BarnetilleggBehovLøser(rapidsConnection, opplysningRepository, søknadRepository, seksjonRepository),
            BarnetilleggV2 to BarnetilleggV2BehovLøser(rapidsConnection, opplysningRepository, søknadRepository),
            AndreØkonomiskeYtelser to AndreØkonomiskeYtelserBehovløser(rapidsConnection, opplysningRepository, søknadRepository),
            ØnsketArbeidstid to ØnsketArbeidstidBehovløser(rapidsConnection, opplysningRepository, søknadRepository),
            HarTilleggsopplysninger to
                HarTilleggsopplysningerBehovløser(rapidsConnection, opplysningRepository, søknadRepository, seksjonRepository),
            BostedslandErNorge to BostedslandErNorgeBehovløser(rapidsConnection, opplysningRepository, søknadRepository),
            PermittertGrensearbeider to
                PermittertGrensearbeiderBehovløser(
                    rapidsConnection,
                    opplysningRepository,
                    søknadRepository,
                ),
            EgetGårdsbruk to EgetGårdsbrukBehovløser(rapidsConnection, opplysningRepository, søknadRepository),
            EgenNæringsvirksomhet to EgenNæringsvirksomhetBehovløser(rapidsConnection, opplysningRepository, søknadRepository),
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
        BarnetilleggV2,
        AndreØkonomiskeYtelser,
        ØnsketArbeidstid,
        HarTilleggsopplysninger,
        BostedslandErNorge,
        PermittertGrensearbeider,
        EgetGårdsbruk,
        EgenNæringsvirksomhet,
    }
}
