package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import no.nav.dagpenger.soknad.orkestrator.behov.Behovløser
import no.nav.dagpenger.soknad.orkestrator.meldinger.BehovMelding
import no.nav.dagpenger.soknad.orkestrator.opplysning.asListOf
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.ArbeidsforholdSvar
import no.nav.dagpenger.soknad.orkestrator.opplysning.db.OpplysningRepository
import no.nav.helse.rapids_rivers.RapidsConnection
import java.util.UUID

class JobbetUtenforNorgeBehovløser(
    rapidsConnection: RapidsConnection,
    opplysningRepository: OpplysningRepository,
) :
    Behovløser(rapidsConnection, opplysningRepository) {
    override val behov = "JobbetUtenforNorge"
    override val beskrivendeId = "faktum.arbeidsforhold"
    private val landkodeNorge = "NOR"

    override fun løs(behovMelding: BehovMelding) {
        val svarPåBehov = harJobbetUtenforNorge(behovMelding.ident, behovMelding.søknadId)
        publiserLøsning(behovMelding, svarPåBehov)
    }

    internal fun harJobbetUtenforNorge(
        ident: String,
        søknadId: UUID,
    ): Boolean {
        val arbeidsforholdOpplysning = opplysningRepository.hent(beskrivendeId, ident, søknadId)

        if (arbeidsforholdOpplysning == null) {
            logger.info { "Søknad med id: $søknadId inneholder ingen opplysninger om arbeidsforhold." }
            return false
        }

        val arbeidsforholdSvar = arbeidsforholdOpplysning.svar.asListOf<ArbeidsforholdSvar>()

        return arbeidsforholdSvar.any { it.land != landkodeNorge }
    }
}
