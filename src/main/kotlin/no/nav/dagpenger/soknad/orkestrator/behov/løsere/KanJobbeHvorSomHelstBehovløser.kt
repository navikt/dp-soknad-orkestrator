package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import mu.KotlinLogging
import no.nav.dagpenger.soknad.orkestrator.behov.Behovsløser
import no.nav.dagpenger.soknad.orkestrator.behov.MeldingOmBehovløsning
import no.nav.dagpenger.soknad.orkestrator.opplysning.db.OpplysningRepository
import no.nav.helse.rapids_rivers.RapidsConnection
import java.util.UUID

class KanJobbeHvorSomHelstBehovløser(
    rapidsConnection: RapidsConnection,
    val opplysningRepository: OpplysningRepository,
) :
    Behovsløser(rapidsConnection) {
    private val beskrivendeId = "faktum.jobbe-hele-norge"
    override val behov = "KanJobbeHvorSomHelst"

    override fun løs(
        ident: String,
        søknadId: UUID,
    ) {
        val svar =
            opplysningRepository.hent(
                beskrivendeId = beskrivendeId,
                ident = ident,
                søknadId = søknadId,
            ).svar

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

    private companion object {
        private val logger = KotlinLogging.logger {}
        private val sikkerlogg = KotlinLogging.logger("tjenestekall.SøknadMottak")
    }
}
