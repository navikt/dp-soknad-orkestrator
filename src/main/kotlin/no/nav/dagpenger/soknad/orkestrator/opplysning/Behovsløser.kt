package no.nav.dagpenger.soknad.orkestrator.opplysning

import no.nav.helse.rapids_rivers.RapidsConnection
import java.util.UUID

abstract class Behovsløser(val rapidsConnection: RapidsConnection) {
    abstract val behov: String

    abstract fun løs(
        ident: String,
        søknadsId: UUID,
        behandlingsId: UUID,
    )
}

class ØnskerDagpengerFraDatoBehovløser(
    rapidsConnection: RapidsConnection,
    val opplysningRepository: OpplysningRepository,
) : Behovsløser(rapidsConnection) {
    private val beskrivendeId = "dagpenger-soknadsdato"
    override val behov = "ØnskerDagpengerFraDato"

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
