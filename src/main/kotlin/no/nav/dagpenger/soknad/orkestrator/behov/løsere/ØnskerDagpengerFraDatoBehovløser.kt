package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.dagpenger.soknad.orkestrator.behov.Behovløser
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.ØnskerDagpengerFraDato
import no.nav.dagpenger.soknad.orkestrator.behov.Behovmelding
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonRepository

class ØnskerDagpengerFraDatoBehovløser(
    rapidsConnection: RapidsConnection,
    opplysningRepository: QuizOpplysningRepository,
    søknadRepository: SøknadRepository,
    seksjonRepository: SeksjonRepository,
) : Behovløser(rapidsConnection, opplysningRepository, søknadRepository, seksjonRepository) {
    override val behov = ØnskerDagpengerFraDato.name
    override val beskrivendeId
        get() = throw NotImplementedError("Overrider løs() og trenger ikke beskrivendeId")
    val beskrivendeIdSøknadsdato = "faktum.dagpenger-soknadsdato"
    val beskrivendeIdGjenopptaksdato = "faktum.arbeidsforhold.gjenopptak.soknadsdato-gjenopptak"
    val dagpengerFraDatoFelt = "hvilkenDatoSøkerDuDagpengerFra"
    val gjenopptakFraDatoFelt = "hvilkenDatoSøkerDuGjenopptakFra"

    override fun løs(behovmelding: Behovmelding) {
        val svarPåBehov =
            opplysningRepository.hent(beskrivendeIdSøknadsdato, behovmelding.ident, behovmelding.søknadId)?.svar
                ?: opplysningRepository.hent(beskrivendeIdGjenopptaksdato, behovmelding.ident, behovmelding.søknadId)?.svar

        if (svarPåBehov != null) {
            return publiserLøsning(behovmelding, svarPåBehov)
        }

        val seksjonsSvar =
            seksjonRepository.hentSeksjonsvarEllerKastException(
                behovmelding.ident,
                behovmelding.søknadId,
                "din-situasjon",
            )

        objectMapper.readTree(seksjonsSvar).let { seksjonssJson ->
            val dagpengerFraDato = seksjonssJson.findPath(dagpengerFraDatoFelt)
            val gjenopptakFraDato = seksjonssJson.findPath(gjenopptakFraDatoFelt)

            if (!dagpengerFraDato.isMissingOrNull()) {
                return publiserLøsning(behovmelding, dagpengerFraDato.asLocalDate())
            } else if (!gjenopptakFraDato.isMissingOrNull()) {
                return publiserLøsning(behovmelding, gjenopptakFraDato.asLocalDate())
            }
        }

        throw IllegalStateException(
            "Fant ingen opplysning på behov $behov for søknad med id: ${behovmelding.søknadId}",
        )
    }
}
