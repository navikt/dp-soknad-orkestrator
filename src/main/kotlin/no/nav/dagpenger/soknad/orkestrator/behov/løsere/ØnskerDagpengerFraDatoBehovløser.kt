package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import mu.KotlinLogging
import no.nav.dagpenger.soknad.orkestrator.behov.Behovsløser
import no.nav.dagpenger.soknad.orkestrator.behov.MeldingOmBehovløsning
import no.nav.dagpenger.soknad.orkestrator.opplysning.db.OpplysningRepository
import no.nav.helse.rapids_rivers.RapidsConnection
import java.util.UUID

class ØnskerDagpengerFraDatoBehovløser(
    rapidsConnection: RapidsConnection,
    private val opplysningRepository: OpplysningRepository,
) : Behovsløser(rapidsConnection) {
    private val beskrivendeId = "dagpenger-soknadsdato"
    override val behov = "ØnskerDagpengerFraDato"

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

        logger.info { "Løste behov $behov for søknad med id: $søknadsId" }
        sikkerlogg.info { "Løste behov $behov for søknad med id: $søknadsId og ident: $ident" }
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
        private val sikkerlogg = KotlinLogging.logger("tjenestekall.SøknadMottak")
    }
}
