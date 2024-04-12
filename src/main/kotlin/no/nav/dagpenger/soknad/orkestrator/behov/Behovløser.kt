package no.nav.dagpenger.soknad.orkestrator.behov

import mu.KotlinLogging
import no.nav.dagpenger.soknad.orkestrator.metrikker.Metrikker
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
        val svarPåBehov =
            opplysningRepository.hent(beskrivendeId, ident, søknadId)?.svar
                ?: throw IllegalStateException(
                    "Fant ingen opplysning med beskrivendeId: $beskrivendeId " +
                        "og kan ikke svare på behov $behov for søknad med id: $søknadId",
                )

        publiserLøsning(ident, søknadId, svarPåBehov)
        Metrikker.behovLost.labels(behov).inc()
    }

    internal fun opprettMeldingMedLøsning(
        ident: String,
        søknadId: UUID,
        svarPåBehov: Any,
    ) = MeldingOmBehovløsning(
        ident = ident,
        søknadId = søknadId,
        løsning =
            mapOf(
                behov to mapOf("verdi" to svarPåBehov),
            ),
    ).asMessage().toJson()

    internal fun publiserLøsning(
        ident: String,
        søknadId: UUID,
        svarPåBehov: Any,
    ) {
        rapidsConnection.publish(opprettMeldingMedLøsning(ident, søknadId, svarPåBehov))

        logger.info { "Løste behov $behov for søknad med id: $søknadId" }
        sikkerlogg.info { "Løste behov $behov for søknad med id: $søknadId og ident: $ident" }
    }

    internal companion object {
        val logger = KotlinLogging.logger {}
        val sikkerlogg = KotlinLogging.logger("tjenestekall.Behovløser")
    }
}
