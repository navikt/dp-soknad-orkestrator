package no.nav.dagpenger.soknad.orkestrator.behov

import mu.KotlinLogging
import no.nav.dagpenger.soknad.orkestrator.opplysning.db.OpplysningRepository
import no.nav.helse.rapids_rivers.RapidsConnection
import java.util.UUID

abstract class Behovløser(val rapidsConnection: RapidsConnection, val opplysningRepository: OpplysningRepository) {
    abstract val behov: String
    abstract val beskrivendeId: String

    internal open fun løs(
        ident: String,
        søknadId: UUID,
    ) {
        val meldingMedLøsning = opprettMeldingMedLøsning(ident, søknadId)
        rapidsConnection.publish(meldingMedLøsning)

        logger.info { "Løste behov $behov for søknad med id: $søknadId" }
        sikkerlogg.info { "Løste behov $behov for søknad med id: $søknadId og ident: $ident" }
    }

    private fun opprettMeldingMedLøsning(
        ident: String,
        søknadId: UUID,
    ) = MeldingOmBehovløsning(
        ident = ident,
        søknadId = søknadId,
        løsning =
            mapOf(
                behov to mapOf("verdi" to hentSvar(ident, søknadId)),
            ),
    ).asMessage().toJson()

    private fun hentSvar(
        ident: String,
        søknadId: UUID,
    ) = opplysningRepository.hent(
        beskrivendeId = beskrivendeId,
        ident = ident,
        søknadId = søknadId,
    ).svar

    internal companion object {
        val logger = KotlinLogging.logger {}
        val sikkerlogg = KotlinLogging.logger("tjenestekall.Behovløser")
    }
}
