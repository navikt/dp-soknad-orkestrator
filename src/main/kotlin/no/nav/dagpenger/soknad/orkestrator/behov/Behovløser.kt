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

    internal open fun løs(packet: JsonMessage) {
        val svarPåBehov =
            opplysningRepository.hent(beskrivendeId, packet.ident(), packet.søknadId())?.svar
                ?: throw IllegalStateException(
                    "Fant ingen opplysning med beskrivendeId: $beskrivendeId " +
                        "og kan ikke svare på behov $behov for søknad med id: ${packet.søknadId()}",
                )

        publiserLøsning(packet, svarPåBehov)
        BehovMetrikker.løst.labels(behov).inc()
    }

    internal fun opprettMeldingMedLøsning(
        packet: JsonMessage,
        svarPåBehov: Any,
    ) = packet.apply {
        this["@løsning"] = mapOf(behov to mapOf("verdi" to svarPåBehov))
    }.toJson()

    internal fun publiserLøsning(
        packet: JsonMessage,
        svarPåBehov: Any,
    ) {
        rapidsConnection.publish(opprettMeldingMedLøsning(packet, svarPåBehov))

        logger.info { "Løste behov $behov for søknad med id: ${packet.søknadId()}" }
        sikkerlogg.info { "Løste behov $behov for søknad med id: ${packet.søknadId()} og ident: ${packet.ident()}" }
    }

    internal companion object {
        val logger = KotlinLogging.logger {}
        val sikkerlogg = KotlinLogging.logger("tjenestekall.Behovløser")
    }

    fun JsonMessage.søknadId(): UUID = UUID.fromString(get("søknad_id").asText())

    fun JsonMessage.ident(): String = get("ident").asText()
}
