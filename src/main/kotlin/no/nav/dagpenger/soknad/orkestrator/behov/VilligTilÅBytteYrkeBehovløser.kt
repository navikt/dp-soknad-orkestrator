package no.nav.dagpenger.soknad.orkestrator.behov

import no.nav.dagpenger.soknad.orkestrator.opplysning.db.OpplysningRepository
import no.nav.helse.rapids_rivers.RapidsConnection
import java.util.UUID

class VilligTilÅBytteYrkeBehovløser(
    rapidsConnection: RapidsConnection,
    val opplysningRepository: OpplysningRepository,
) :
    Behovsløser(rapidsConnection) {
    private val beskrivendeId = "bytte-yrke-ned-i-lonn"
    override val behov = "VilligTilÅBytteYrke"

    override fun løs(
        ident: String,
        søknadsId: UUID,
    ) {
        val svar =
            opplysningRepository.hent(
                beskrivendeId = beskrivendeId,
                ident = ident,
                søknadsId = søknadsId,
            ).svar

        val løsning =
            MeldingOmBehovLøsning(
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
}
