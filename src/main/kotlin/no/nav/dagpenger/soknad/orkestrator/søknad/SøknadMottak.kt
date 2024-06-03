package no.nav.dagpenger.soknad.orkestrator.søknad

import com.fasterxml.jackson.databind.JsonNode
import mu.KotlinLogging
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import no.nav.dagpenger.soknad.orkestrator.metrikker.SøknadMetrikker
import no.nav.dagpenger.soknad.orkestrator.opplysning.db.OpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.utils.asUUID
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.withMDC

class SøknadMottak(
    rapidsConnection: RapidsConnection,
    private val søknadService: SøknadService,
    private val opplysningRepository: OpplysningRepository,
    private val søknadRepository: SøknadRepository,
) :
    River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "søknad_innsendt_varsel") }
            validate { it.requireKey("ident", "søknadId", "søknadstidspunkt", "søknadData") }
            validate { it.interestedIn("@id", "@opprettet") }
        }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        withMDC(
            mapOf("søknadId" to packet["søknadId"].asText()),
        ) {
            logger.info { "Mottok søknad innsendt hendelse for søknad ${packet["søknadId"]}" }
            SøknadMetrikker.mottatt.inc()
            sikkerlogg.info { "Mottok søknad innsendt hendelse: ${packet.toJson()}" }

            val jsonNode = objectMapper.readTree(packet.toJson())

            with(jsonNode) {
                tilSøknad()
                    .also(søknadRepository::lagre)

                tilSøknadstidspunkt()
                    .also(opplysningRepository::lagre)

                publiserMeldingOmSøknadInnsendt()
            }
        }
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
        private val sikkerlogg = KotlinLogging.logger("tjenestekall.SøknadMottak")
    }

    private fun JsonNode.tilSøknad() = SøknadMapper(this).søknad

    private fun JsonNode.tilSøknadstidspunkt() = SøknadtidspunktMapper(this).tidspunktOpplysning

    private fun JsonNode.publiserMeldingOmSøknadInnsendt() {
        val ident = this.get("ident").asText()
        val søknadId = this.get("søknadId").asUUID()

        søknadService.publiserMeldingOmSøknadInnsendt(søknadId, ident)
    }
}
