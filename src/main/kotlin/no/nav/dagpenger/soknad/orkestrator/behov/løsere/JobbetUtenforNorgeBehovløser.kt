package no.nav.dagpenger.soknad.orkestrator.behov.løsere

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
        søknadsId: UUID,
    ) {
        val svar = harJobbetUtenforNorge(ident, søknadsId)

        val løsning =
            MeldingOmBehovløsning(
                ident = ident,
                søknadsId = søknadsId,
                løsning =
                    mapOf(
                        behov to
                            mapOf(
                                "verdi" to svar,
                            ),
                    ),
            ).asMessage().toJson()

        rapidsConnection.publish(løsning)
    }

    internal fun harJobbetUtenforNorge(
        ident: String,
        søknadsId: UUID,
    ): Boolean {
        val arbeidsforholdSvar =
            opplysningRepository.hent(beskrivendeId, ident, søknadsId).svar as List<ArbeidsforholdSvar>

        return arbeidsforholdSvar.any { it.land != landkodeNorge }
    }
}
