package no.nav.dagpenger.soknad.orkestrator.søknad.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.withMDC
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.soknad.orkestrator.søknad.SøknadService
import no.nav.dagpenger.soknad.orkestrator.utils.asUUID

class SøknadSlettetMottak(
    rapidsConnection: RapidsConnection,
    private val søknadService: SøknadService,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition {
                    it.requireValue("@event_name", "søknad_slettet")
                }
                validate {
                    it.requireKey("søknad_uuid", "ident")
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
            mapOf("søknadId" to packet["søknad_uuid"].asText()),
        ) {
            logger.info { "Mottok søknad slettet hendelse for søknad ${packet["søknad_uuid"]}" }
            sikkerlogg.info { "Mottok søknad slettet hendelse: ${packet.toJson()}" }

            val søknadId = packet["søknad_uuid"].asUUID()
            val ident = packet["ident"].asText()

            søknadService.slettSøknadOgInkrementerMetrikk(søknadId, ident)
        }
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
        private val sikkerlogg = KotlinLogging.logger("tjenestekall.SøknadMottak")
    }
}
