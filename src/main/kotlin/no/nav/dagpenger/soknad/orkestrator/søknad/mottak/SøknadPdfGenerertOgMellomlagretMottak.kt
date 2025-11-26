package no.nav.dagpenger.soknad.orkestrator.søknad.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import no.nav.dagpenger.soknad.orkestrator.søknad.SøknadService
import no.nav.dagpenger.soknad.orkestrator.søknad.behov.BehovForJournalføringAvSøknadPdfOgVedlegg
import no.nav.dagpenger.soknad.orkestrator.søknad.dokumentVarianter
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonService
import no.nav.dagpenger.soknad.orkestrator.utils.asUUID

internal class SøknadPdfGenerertOgMellomlagretMottak(
    val rapidsConnection: RapidsConnection,
    val søknadService: SøknadService,
    val seksjonService: SeksjonService,
) : River.PacketListener {
    companion object {
        private val logg = KotlinLogging.logger {}
        private val sikkerLogg = KotlinLogging.logger("tjenestekall.${this::class.simpleName}")
        private const val BEHOV = "generer_og_mellomlagre_søknad_pdf"
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
        withLoggingContext(
            "søknadId" to søknadId.toString(),
        ) {
            logg.info { "Mottok løsning for $BEHOV for søknad $søknadId" }
            sikkerLogg.info { "Mottok løsning for $BEHOV for søknad $søknadId innsendt av $ident" }
            val søknadsData = seksjonService.hentAlleSeksjonerMedSeksjonIdSomNøkkel(ident, søknadId)
            val behovForJournalføringAvSøknadPdfOgVedlegg =
                BehovForJournalføringAvSøknadPdfOgVedlegg(
                    søknadId = søknadId,
                    ident = ident,
                    dokumentvarianter = packet["@løsning"][BEHOV].dokumentVarianter(),
                    dokumenter = søknadService.opprettDokumenterFraDokumentasjonskrav(søknadId, ident),
                    data = objectMapper.writeValueAsString(søknadsData),
                )

            rapidsConnection.publish(ident, behovForJournalføringAvSøknadPdfOgVedlegg.asMessage().toJson())
            logg.info { "Publiserte melding om behov for journalføring av søknad-PDF og vedlegg for søknad $søknadId " }
            sikkerLogg.info {
                "Publiserte melding om behov for journalføring av søknad-PDF og vedlegg for søknad $søknadId innsendt av $ident"
            }
        }
    }
}
