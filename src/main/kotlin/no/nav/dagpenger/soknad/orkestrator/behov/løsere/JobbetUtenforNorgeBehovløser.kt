package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import no.nav.dagpenger.soknad.orkestrator.behov.Behovløser
import no.nav.dagpenger.soknad.orkestrator.behov.MeldingOmBehovløsning
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

    override fun løs(
        ident: String,
        søknadId: UUID,
    ) {
        val svar = harJobbetUtenforNorge(ident, søknadId)

        val meldingMedLøsning = opprettMeldingMedLøsning(ident, søknadId, svar)

        rapidsConnection.publish(meldingMedLøsning)

        logger.info { "Løste behov $behov for søknad med id: $søknadId" }
        sikkerlogg.info { "Løste behov $behov for søknad med id: $søknadId og ident: $ident" }
    }

    internal fun harJobbetUtenforNorge(
        ident: String,
        søknadId: UUID,
    ): Boolean {
        val arbeidsforholdSvar =
            opplysningRepository.hent(beskrivendeId, ident, søknadId).svar.asListOf<ArbeidsforholdSvar>()

        return arbeidsforholdSvar.any { it.land != landkodeNorge }
    }

    private fun opprettMeldingMedLøsning(
        ident: String,
        søknadId: UUID,
        svar: Boolean,
    ) = MeldingOmBehovløsning(
        ident = ident,
        søknadId = søknadId,
        løsning =
            mapOf(
                behov to
                    mapOf(
                        "verdi" to svar,
                    ),
            ),
    ).asMessage().toJson()
}
