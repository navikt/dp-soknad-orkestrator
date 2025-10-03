package no.nav.dagpenger.soknad.orkestrator.journalføring

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.withMDC
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import no.nav.dagpenger.soknad.orkestrator.metrikker.SøknadMetrikker
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonService

class JournalførSøknadHtmlMottak(
    rapidsConnection: RapidsConnection,
    private val seksjonService: SeksjonService,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition {
                    it.requireValue("@event_name", "journalfor_søknad_html")
                }
                validate {
                    it.requireKey("ident", "søknadId", "søknadHtml")
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
            logger.info { "Mottok journalfør søknad-hendelse for søknad ${packet["søknadId"]}" }
            SøknadMetrikker.mottatt.inc()
            sikkerlogg.info { "Mottok journalfør søknad-hendelse: ${packet.toJson()}" }

            val melding = objectMapper.readTree(packet.toJson())
            val htmlStreng = melding["søknadHtml"].asText()

            seksjonService.journalførSøknadHtml(htmlStreng)
        }
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
        private val sikkerlogg = KotlinLogging.logger("tjenestekall.SøknadMottak")
    }
}
