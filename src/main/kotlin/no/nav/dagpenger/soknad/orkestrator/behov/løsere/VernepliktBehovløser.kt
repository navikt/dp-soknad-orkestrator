package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.dagpenger.soknad.orkestrator.behov.Behovløser
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.Verneplikt
import no.nav.dagpenger.soknad.orkestrator.behov.Behovmelding
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonRepository
import no.nav.dagpenger.soknad.orkestrator.utils.erBoolean

class VernepliktBehovløser(
    rapidsConnection: RapidsConnection,
    opplysningRepository: QuizOpplysningRepository,
    søknadRepository: SøknadRepository,
    seksjonRepository: SeksjonRepository,
) : Behovløser(rapidsConnection, opplysningRepository, søknadRepository, seksjonRepository) {
    override val behov = Verneplikt.name
    override val beskrivendeId = "faktum.avtjent-militaer-sivilforsvar-tjeneste-siste-12-mnd"

    override fun løs(behovmelding: Behovmelding) {
        val svarPåBehov =
            opplysningRepository.hent(beskrivendeId, behovmelding.ident, behovmelding.søknadId)?.svar

        if (svarPåBehov != null) {
            publiserLøsning(behovmelding, svarPåBehov)
        } else {
            val seksjonsSvar =
                seksjonRepository?.hentSeksjonsvar(
                    behovmelding.ident,
                    behovmelding.søknadId,
                    "verneplikt",
                ) ?: throw IllegalStateException(
                    "Fant ingen opplysning med beskrivendeId: $beskrivendeId " +
                        "og kan ikke svare på behov $behov for søknad med id: ${behovmelding.søknadId}",
                )

            objectMapper.readTree(seksjonsSvar).let { seksjonsJson ->
                seksjonsJson.findPath("avtjent-verneplikt")?.let {
                    if (!it.isMissingOrNull()) {
                        publiserLøsning(behovmelding, it.erBoolean())
                    }
                }
            }

            throw IllegalStateException(
                "Fant ingen opplysning på behov $behov for søknad med id: ${behovmelding.søknadId}",
            )
        }
    }
}
