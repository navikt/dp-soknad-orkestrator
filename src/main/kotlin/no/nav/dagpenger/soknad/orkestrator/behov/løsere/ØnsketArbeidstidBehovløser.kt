package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.dagpenger.soknad.orkestrator.behov.Behovløser
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.ØnsketArbeidstid
import no.nav.dagpenger.soknad.orkestrator.behov.Behovmelding
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonRepository

class ØnsketArbeidstidBehovløser(
    rapidsConnection: RapidsConnection,
    opplysningRepository: QuizOpplysningRepository,
    søknadRepository: SøknadRepository,
    seksjonRepository: SeksjonRepository,
) : Behovløser(rapidsConnection, opplysningRepository, søknadRepository, seksjonRepository) {
    override val behov = ØnsketArbeidstid.name
    override val beskrivendeId = "faktum.kun-deltid-aarsak-antall-timer"

    override fun løs(behovmelding: Behovmelding) {
        val opplysning =
            opplysningRepository.hent(beskrivendeId, behovmelding.ident, behovmelding.søknadId)

        if (opplysning != null) {
            val svarPåBehov = opplysning?.svar ?: 40.0
            return publiserLøsning(behovmelding, svarPåBehov)
        }

        var seksjonsSvar =
            seksjonRepository?.hentSeksjonsvar(
                ident = behovmelding.ident,
                søknadId = behovmelding.søknadId,
                seksjonId = "reell-arbeidssoker",
            ) ?: throw IllegalStateException(
                "Fant ingen seksjonsvar på Reell Arbeidssøker for søknad=${behovmelding.søknadId}",
            )

        objectMapper.readTree(seksjonsSvar).let { seksjonsJson ->
            seksjonsJson.findPath("kan-ikke-jobbe-både-heltid-og-deltid-antall-timer")?.let {
                if (!it.isMissingOrNull()) {
                    return publiserLøsning(behovmelding, it)
                }
            }
        }
        return publiserLøsning(behovmelding, 40.0)
    }
}
