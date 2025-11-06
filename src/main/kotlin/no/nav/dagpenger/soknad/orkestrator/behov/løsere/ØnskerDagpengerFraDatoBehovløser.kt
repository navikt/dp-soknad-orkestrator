package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.dagpenger.soknad.orkestrator.behov.Behovløser
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.ØnskerDagpengerFraDato
import no.nav.dagpenger.soknad.orkestrator.behov.Behovmelding
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository

class ØnskerDagpengerFraDatoBehovløser(
    rapidsConnection: RapidsConnection,
    opplysningRepository: QuizOpplysningRepository,
    søknadRepository: SøknadRepository,
) : Behovløser(rapidsConnection, opplysningRepository, søknadRepository) {
    override val behov = ØnskerDagpengerFraDato.name
    override val beskrivendeId
        get() = throw NotImplementedError("Overrider løs() og trenger ikke beskrivendeId")
    val beskrivendeIdSøknadsdato = "faktum.dagpenger-soknadsdato"
    val beskrivendeIdGjenopptaksdato = "faktum.arbeidsforhold.gjenopptak.soknadsdato-gjenopptak"

    override fun løs(behovmelding: Behovmelding) {
        val svarPåBehov =
            opplysningRepository.hent(beskrivendeIdSøknadsdato, behovmelding.ident, behovmelding.søknadId)?.svar
                ?: opplysningRepository.hent(beskrivendeIdGjenopptaksdato, behovmelding.ident, behovmelding.søknadId)?.svar
                ?: throw IllegalStateException(
                    "Fant ingen opplysning med beskrivendeId: $beskrivendeIdSøknadsdato eller $beskrivendeIdGjenopptaksdato " +
                        "og kan ikke svare på behov $behov for søknad med id: ${behovmelding.søknadId}",
                )

        publiserLøsning(behovmelding, svarPåBehov)
    }
}
