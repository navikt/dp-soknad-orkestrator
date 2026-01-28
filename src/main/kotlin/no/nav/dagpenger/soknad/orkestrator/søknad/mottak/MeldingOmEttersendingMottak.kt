package no.nav.dagpenger.soknad.orkestrator.søknad.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonRepository
import no.nav.dagpenger.soknad.orkestrator.utils.asUUID

internal class MeldingOmEttersendingMottak(
    val rapidsConnection: RapidsConnection,
    val søknadRepository: SøknadRepository,
    val seksjonRepository: SeksjonRepository,
) : River.PacketListener {
    companion object {
        private val logg = KotlinLogging.logger {}
        private val sikkerLogg = KotlinLogging.logger("tjenestekall.${this::class.simpleName}")
        const val BEHOV = "journalfør_ettersending_av_dokumentasjon"
    }

    init {
        River(rapidsConnection)
            .apply {
                precondition {
                    it.requireValue("@event_name", "behov")
                    it.requireAllOrAny("@behov", listOf(BEHOV))
                    it.requireKey("@løsning")
                    it.requireValue("@final", true)
                }
                validate {
                    it.requireKey("søknadId", "ident")
                    it.require("@løsning") { løsning ->
                        løsning.required(BEHOV)
                    }
                    it.require("@løsning.$BEHOV") { behov ->
                        behov.required("journalpostId")
                        behov.required("journalførtTidspunkt")
                        behov.required("dokumentasjonskravJson")
                        behov.required("seksjonId")
                    }
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
        val ident = packet["ident"].asText()
        val dokumentasjonskravJson =
            packet["@løsning"][BEHOV]["dokumentasjonskravJson"].asText()
        val seksjonId = packet["@løsning"][BEHOV]["seksjonId"].asText()

        println("Mottok melding om ettersending for søknad $søknadId fra $ident")

        withLoggingContext(
            "søknadId" to søknadId.toString(),
        ) {
            logg.info { "Mottok løsning for $BEHOV for søknad $søknadId" }
            sikkerLogg.info { "Mottok løsning for $BEHOV for søknad $søknadId innsendt av $ident" }

            søknadRepository.verifiserAtSøknadEksistererOgTilhørerIdent(søknadId, ident)
            seksjonRepository.lagreDokumentasjonskravEttersending(
                søknadId = søknadId,
                ident = ident,
                seksjonId = seksjonId,
                dokumentasjonskrav = dokumentasjonskravJson,
            )
        }
    }
}
