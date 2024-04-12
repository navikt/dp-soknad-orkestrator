package no.nav.dagpenger.soknad.orkestrator.søknad

import com.fasterxml.jackson.databind.JsonNode
import mu.KotlinLogging
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import no.nav.dagpenger.soknad.orkestrator.metrikker.Metrikker
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysning
import no.nav.dagpenger.soknad.orkestrator.opplysning.datatyper.Tekst
import no.nav.dagpenger.soknad.orkestrator.opplysning.db.OpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.utils.asUUID
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

class SøknadMottak(
    rapidsConnection: RapidsConnection,
    private val søknadService: SøknadService,
    private val opplysningRepository: OpplysningRepository,
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
        logger.info { "Mottok søknad innsendt hendelse for søknad ${packet["søknadId"]}" }
        Metrikker.sokaderMottatt.inc()
        sikkerlogg.info { "Mottok søknad innsendt hendelse: ${packet.toJson()}" }

        val jsonNode = objectMapper.readTree(packet.toJson())

        with(jsonNode) {
            søknadTidspunktMapper()
                .also(opplysningRepository::lagre)

            søknadMapper()
                .also { it.opplysninger.forEach(opplysningRepository::lagre) }
                .also(søknadService::publiserMeldingOmSøknadInnsendt)
        }
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
        private val sikkerlogg = KotlinLogging.logger("tjenestekall.SøknadMottak")
    }

    private fun JsonNode.søknadMapper() = SøknadMapper(this).søknad

    private fun JsonNode.søknadTidspunktMapper(): Opplysning<String> {
        val ident = this.get("ident").asText()
        val søknadId = this.get("søknadId").asUUID()
        val søknadstidspunkt = this.get("søknadstidspunkt").asText()

        return Opplysning(
            beskrivendeId = "søknadstidspunkt",
            type = Tekst,
            svar = søknadstidspunkt,
            ident = ident,
            søknadId = søknadId,
        )
    }
}
