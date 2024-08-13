package no.nav.dagpenger.soknad.orkestrator.behov

import mu.KotlinLogging
import no.nav.dagpenger.soknad.orkestrator.metrikker.BehovMetrikker
import no.nav.dagpenger.soknad.orkestrator.opplysning.db.OpplysningRepository
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import java.util.UUID

abstract class Behovløser(val rapidsConnection: RapidsConnection, val opplysningRepository: OpplysningRepository) {
    abstract val behov: String
    abstract val beskrivendeId: String

    internal open fun løs(behovmelding: Behovmelding) {
        val svarPåBehov =
            opplysningRepository.hent(beskrivendeId, behovmelding.ident, behovmelding.søknadId)?.svar ?: throw IllegalStateException(
                "Fant ingen opplysning med beskrivendeId: $beskrivendeId " +
                    "og kan ikke svare på behov $behov for søknad med id: ${behovmelding.søknadId}",
            )

        publiserLøsning(behovmelding, svarPåBehov)
    }

    internal fun publiserLøsning(
        behovmelding: Behovmelding,
        svarPåBehov: Any,
    ) {
        behovmelding.innkommendePacket["@løsning"] = mapOf(behov to mapOf("verdi" to svarPåBehov))
        rapidsConnection.publish(behovmelding.ident, behovmelding.innkommendePacket.toJson())

        BehovMetrikker.løst.labels(behov).inc()
        logger.info { "Løste behov $behov" }
        sikkerlogg.info { "Løste behov $behov med løsning: $svarPåBehov" }
    }

    internal companion object {
        val logger = KotlinLogging.logger {}
        val sikkerlogg = KotlinLogging.logger("tjenestekall.Behovløser")
    }

    fun JsonMessage.søknadId(): UUID = UUID.fromString(get("søknadId").asText())

    fun JsonMessage.ident(): String = get("ident").asText()
}
