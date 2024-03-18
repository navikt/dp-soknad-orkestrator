package no.nav.dagpenger.soknad.orkestrator.behov

import no.nav.dagpenger.soknad.orkestrator.opplysning.db.OpplysningRepository
import no.nav.helse.rapids_rivers.RapidsConnection
import java.util.UUID

class KanJobbeDeltidBehovløser(
    rapidsConnection: RapidsConnection,
    val opplysningRepository: OpplysningRepository,
) :
    Behovsløser(rapidsConnection) {
    private val beskrivendeId = "jobbe-hel-deltid"
    override val behov = "KanJobbeDeltid"

    override fun løs(
        ident: String,
        søknadsId: UUID,
        behandlingsId: UUID,
    ) {
        val svar =
            opplysningRepository.hent(
                beskrivendeId = beskrivendeId,
                ident = ident,
                søknadsId = søknadsId,
                behandlingsId = behandlingsId,
            ).svar

        val løsning =
            MeldingOmBehovLøsning(
                ident = ident,
                søknadsId = søknadsId,
                behandlingsId = behandlingsId,
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
}
