package no.nav.dagpenger.soknad.orkestrator.søknad

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.withMDC
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import mu.KotlinLogging
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import no.nav.dagpenger.soknad.orkestrator.metrikker.SøknadMetrikker
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.utils.asUUID

class SøknadMottak(
    rapidsConnection: RapidsConnection,
    private val søknadService: SøknadService,
    private val søknadRepository: SøknadRepository,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition {
                    it.requireValue("@event_name", "søknad_innsendt_varsel")
                }
                validate {
                    it.requireKey("ident", "søknadId", "søknadstidspunkt", "søknadData")
                    it.interestedIn("@id", "@opprettet")
                }
            }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        withMDC(
            mapOf("søknadId" to packet["søknadId"].asText()),
        ) {
            logger.info { "Mottok søknad innsendt hendelse for søknad ${packet["søknadId"]}" }
            SøknadMetrikker.mottatt.inc()
            sikkerlogg.info { "Mottok søknad innsendt hendelse: ${packet.toJson()}" }

            val søknadmelding = objectMapper.readTree(packet.toJson())
            søknadRepository.lagreQuizSøknad(søknadmelding.tilSøknad())
            søknadmelding.publiserMeldingOmSøknadInnsendt()

            try {
                val søknaddata = opprettOgLagreKomplettSøknaddata(søknadmelding)
                logger.info { "Komplett søknaddata opprettet for søknad ${packet["søknadId"]}" }
                sikkerlogg.info { "Komplett søknaddata opprettet for søknad ${packet["søknadId"]}: $søknaddata" }
            } catch (e: Exception) {
                logger.error(e) { "Feil ved opprettelse av komplett søknaddata for søknad ${packet["søknadId"]}" }
                sikkerlogg.error(e) { "Feil ved opprettelse av komplett søknaddata. Packet: ${packet.toJson()}" }
            }
        }
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
        private val sikkerlogg = KotlinLogging.logger("tjenestekall.SøknadMottak")
    }

    private fun JsonNode.tilSøknad() = SøknadMapper(this).søknad

    private fun JsonNode.publiserMeldingOmSøknadInnsendt() {
        val ident = this.get("ident").asText()
        val søknadId = this.get("søknadId").asUUID()

        søknadService.publiserMeldingOmSøknadInnsendt(søknadId, ident)
    }

    private fun opprettOgLagreKomplettSøknaddata(søknadMelding: JsonNode): JsonNode {
        val ident = søknadMelding.get("ident").asText()
        val søknadId = søknadMelding.get("søknadId").asUUID()
        val seksjoner = søknadMelding["søknadData"]["seksjoner"]

        return søknadService.opprettOgLagreKomplettSøknaddata(ident, søknadId, seksjoner)
    }
}
