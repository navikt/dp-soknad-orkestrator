package no.nav.dagpenger.soknad.orkestrator.opplysning

import no.nav.helse.rapids_rivers.RapidsConnection
import java.util.UUID

class OpplysningService(private val rapid: RapidsConnection, private val repository: OpplysningRepository) {
    fun hentOpplysning(
        beskrivendeId: String,
        ident: String,
        søknadId: String,
        behandlingId: String,
    ): Opplysning {
        return repository.hent(
            beskrivendeId,
            ident,
            UUID.fromString(søknadId),
            UUID.fromString(behandlingId),
        )
    }

    fun publiserMeldingOmOpplysningBehovLøsning(opplysning: Opplysning) {
        rapid.publish(MeldingOmOpplysningBehovLøsning(opplysning).asMessage().toJson())
    }
}
