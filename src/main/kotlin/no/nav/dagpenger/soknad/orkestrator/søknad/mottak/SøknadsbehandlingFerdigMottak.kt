package no.nav.dagpenger.soknad.orkestrator.søknad.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.soknad.orkestrator.søknad.Status
import no.nav.dagpenger.soknad.orkestrator.søknad.SøknadStatus
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadStatusRepository
import no.nav.dagpenger.soknad.orkestrator.utils.asUUID

class SøknadsbehandlingFerdigMottak(
    val rapidsConnection: RapidsConnection,
    val søknadRepository: SøknadRepository,
    val søknadStatusRepository: SøknadStatusRepository,
) : River.PacketListener {
    companion object {
        private val logg = KotlinLogging.logger {}
        private val sikkerLogg = KotlinLogging.logger("tjenestekall.${this::class.simpleName}")
        const val BEHOV = "søknadsbehandling_ferdig"
    }

    init {
        River(rapidsConnection)
            .apply {
                precondition {
                    it.requireValue("@event_name", BEHOV)
                }
                validate {
                    it.requireKey("søknadId", "ident", "behandlingId", "førteTil", "rettighetsperioder")
                }
            }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val søknadId = packet["søknadId"].asUUID()
        val ident = packet["ident"].asString()
        val førteTil = packet["førteTil"].asString()
        val behandlingId = packet["behandlingId"].asString()
        val rettighetsperioder = packet["rettighetsperioder"].toString()

        logg.info { "Mottok $BEHOV for søknad $søknadId" }
        sikkerLogg.info { "Mottok $BEHOV for søknad $søknadId: ${packet.toJson()}" }

        søknadRepository.hent(søknadId)?.let {
            if (it.ident != ident) {
                sikkerLogg.error { "Søknad $søknadId tilhører ikke ident: $ident for oppdatering av status" }
                throw IllegalArgumentException("Søknad $søknadId tilhører ikke identen for oppdatering av status")
            }
            søknadStatusRepository.lagre(
                søknadStatus =
                    SøknadStatus(
                        søknadId = søknadId,
                        behandlingId = behandlingId,
                        ident = ident,
                        førteTil = Status.valueOf(førteTil),
                        rettighetsperioder = rettighetsperioder,
                    ),
            )
        } ?: also {
            logg.warn { "Fant ikke søknad $søknadId for oppdatering status" }
            sikkerLogg.warn { "Fant ikke søknad $søknadId innsendt av $ident for oppdatering av status" }
        }
    }
}
