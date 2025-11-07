package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.dagpenger.soknad.orkestrator.behov.Behovløser
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.PermittertGrensearbeider
import no.nav.dagpenger.soknad.orkestrator.behov.Behovmelding
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonRepository
import no.nav.dagpenger.soknad.orkestrator.utils.erBoolean
import java.util.UUID

class PermittertGrensearbeiderBehovløser(
    rapidsConnection: RapidsConnection,
    opplysningRepository: QuizOpplysningRepository,
    søknadRepository: SøknadRepository,
    seksjonRepository: SeksjonRepository,
) : Behovløser(rapidsConnection, opplysningRepository, søknadRepository, seksjonRepository) {
    override val behov = PermittertGrensearbeider.name
    override val beskrivendeId
        get() = throw NotImplementedError("Overrider løs() og trenger ikke beskrivendeId")

    companion object {
        val reistTilbakeEnGangEllerMerBeskrivendeId = "faktum.reist-tilbake-en-gang-eller-mer"
        val reistITaktMedRotasjonBeskrivendeId = "faktum.reist-i-takt-med-rotasjon"
    }

    override fun løs(behovmelding: Behovmelding) {
        val svarPåBehov = harReistTilbake(behovmelding.ident, behovmelding.søknadId)
        publiserLøsning(behovmelding, svarPåBehov)
    }

    private fun harReistTilbake(
        ident: String,
        søknadId: UUID,
    ): Boolean {
        val reistTilbakeEnGangEllerMerOpplysning =
            opplysningRepository.hent(reistTilbakeEnGangEllerMerBeskrivendeId, ident, søknadId)

        val reistITaktMedRotasjonOpplysning =
            opplysningRepository.hent(reistITaktMedRotasjonBeskrivendeId, ident, søknadId)

        if (reistTilbakeEnGangEllerMerOpplysning != null || reistITaktMedRotasjonOpplysning != null) {
            return reistTilbakeEnGangEllerMerOpplysning?.svar as? Boolean ?: false ||
                reistITaktMedRotasjonOpplysning?.svar as? Boolean ?: false
        }

        val seksjonsSvar =
            seksjonRepository.hentSeksjonsvar(
                ident,
                søknadId,
                "personalia",
            ) ?: return false

        objectMapper.readTree(seksjonsSvar).let { seksjonsJson ->
            val reistTilbakeEnGangEllerMerOpplysning =
                seksjonsJson.findPath("reiste-du-hjem-til-landet-du-bor-i")?.let {
                    if (!it.isMissingOrNull()) {
                        it.erBoolean()
                    } else {
                        false
                    }
                }

            val reistITaktMedRotasjonOpplysning =
                seksjonsJson.findPath("reiste-du-i-takt-med-rotasjon")?.let {
                    if (!it.isMissingOrNull()) {
                        it.erBoolean()
                    } else {
                        false
                    }
                }

            return reistTilbakeEnGangEllerMerOpplysning as? Boolean ?: false ||
                reistITaktMedRotasjonOpplysning as? Boolean ?: false
        }
    }
}
