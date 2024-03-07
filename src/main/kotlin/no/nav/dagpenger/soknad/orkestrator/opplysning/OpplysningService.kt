package no.nav.dagpenger.soknad.orkestrator.opplysning

import no.nav.helse.rapids_rivers.RapidsConnection
import java.util.UUID

class OpplysningService(private val rapid: RapidsConnection, private val repository: OpplysningRepository) {
    fun hentOpplysning(
        beskrivendeId: String,
        ident: String,
        søknadsId: String,
        behandlingsId: String,
    ): Opplysning {
        return repository.hent(
            beskrivendeId = beskrivendeId,
            ident = ident,
            søknadsId = UUID.fromString(søknadsId),
            behandlingsId = UUID.fromString(behandlingsId),
        )
    }

    fun publiserMeldingOmOpplysningBehovLøsning(opplysning: Opplysning) {
        rapid.publish(MeldingOmOpplysningBehovLøsning(opplysning).asMessage().toJson())
    }
}
