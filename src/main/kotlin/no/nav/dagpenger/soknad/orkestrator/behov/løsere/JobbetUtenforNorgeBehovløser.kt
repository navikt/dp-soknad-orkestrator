package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import mu.KotlinLogging
import no.nav.dagpenger.soknad.orkestrator.behov.Behovsløser
import no.nav.dagpenger.soknad.orkestrator.behov.MeldingOmBehovløsning
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.ArbeidsforholdSvar
import no.nav.dagpenger.soknad.orkestrator.opplysning.db.OpplysningRepository
import no.nav.helse.rapids_rivers.RapidsConnection
import java.util.UUID

class JobbetUtenforNorgeBehovløser(
    rapidsConnection: RapidsConnection,
    val opplysningRepository: OpplysningRepository,
) :
    Behovsløser(rapidsConnection) {
    override val behov = "JobbetUtenforNorge"
    private val landkodeNorge = "NOR"
    private val beskrivendeId = "faktum.arbeidsforhold"

    override fun løs(
        ident: String,
        søknadId: UUID,
    ) {
        val svar = harJobbetUtenforNorge(ident, søknadId)

        val løsning =
            MeldingOmBehovløsning(
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

        rapidsConnection.publish(løsning)

        logger.info { "Løste behov $behov for søknad med id: $søknadId" }
        sikkerlogg.info { "Løste behov $behov for søknad med id: $søknadId og ident: $ident" }
    }

    internal fun harJobbetUtenforNorge(
        ident: String,
        søknadId: UUID,
    ): Boolean {
        val arbeidsforholdSvar =
            opplysningRepository.hent(beskrivendeId, ident, søknadId).svar as List<ArbeidsforholdSvar>

        return arbeidsforholdSvar.any { it.land != landkodeNorge }
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
        private val sikkerlogg = KotlinLogging.logger("tjenestekall.SøknadMottak")
    }
}
