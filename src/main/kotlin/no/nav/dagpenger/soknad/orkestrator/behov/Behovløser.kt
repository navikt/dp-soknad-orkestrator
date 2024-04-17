package no.nav.dagpenger.soknad.orkestrator.behov

import mu.KotlinLogging
import no.nav.dagpenger.soknad.orkestrator.meldinger.BehovMelding
import no.nav.dagpenger.soknad.orkestrator.meldinger.MeldingOmBehovløsning
import no.nav.dagpenger.soknad.orkestrator.metrikker.BehovMetrikker
import no.nav.dagpenger.soknad.orkestrator.opplysning.db.OpplysningRepository
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import java.util.UUID

abstract class Behovløser(val rapidsConnection: RapidsConnection, val opplysningRepository: OpplysningRepository) {
    abstract val behov: String
    abstract val beskrivendeId: String

    internal open fun løs(behovMelding: BehovMelding) {
        val svarPåBehov =
            opplysningRepository.hent(beskrivendeId, behovMelding.ident, behovMelding.søknadId)?.svar
                ?: throw IllegalStateException(
                    "Fant ingen opplysning med beskrivendeId: $beskrivendeId " +
                        "og kan ikke svare på behov $behov for søknad med id: ${behovMelding.søknadId}",
                )

        publiserLøsning(behovMelding, svarPåBehov)
        BehovMetrikker.løst.labels(behov).inc()
    }

    internal fun opprettMeldingMedLøsning(
        behovMelding: BehovMelding,
        svarPåBehov: Any,
    ) = MeldingOmBehovløsning(behovMelding, mapOf(behov to mapOf("verdi" to svarPåBehov))).asMessage().toJson()

    internal fun publiserLøsning(
        behovMelding: BehovMelding,
        svarPåBehov: Any,
    ) {
        rapidsConnection.publish(opprettMeldingMedLøsning(behovMelding, svarPåBehov))

        logger.info { "Løste behov $behov for søknad med id: ${behovMelding.søknadId}" }
        sikkerlogg.info { "Løste behov $behov for søknad med id: ${behovMelding.søknadId} og ident: ${behovMelding.ident}" }
    }

    internal companion object {
        val logger = KotlinLogging.logger {}
        val sikkerlogg = KotlinLogging.logger("tjenestekall.Behovløser")
    }

    fun JsonMessage.søknadId(): UUID = UUID.fromString(get("søknad_id").asText())

    fun JsonMessage.ident(): String = get("ident").asText()
}
