package no.nav.dagpenger.soknad.orkestrator.søknad

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.utils.asUUID

internal class SøknadPdfOgVedleggJournalførtMottak(
    val rapidsConnection: RapidsConnection,
    val søknadRepository: SøknadRepository,
) : River.PacketListener {
    companion object {
        private val logg = KotlinLogging.logger {}
        private val sikkerLogg = KotlinLogging.logger("tjenestekall.${this::class.simpleName}")
        const val BEHOV = "journalfør_søknad_pdf_og_vedlegg"
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
        val journalpostId = packet["@løsning"][BEHOV]["journalpostId"].asText()
        val journalførtTidspunkt = packet["@løsning"][BEHOV]["journalførtTidspunkt"].asLocalDateTime()

        withLoggingContext(
            "søknadId" to søknadId.toString(),
        ) {
            logg.info { "Mottok løsning for $BEHOV for søknad $søknadId" }
            sikkerLogg.info { "Mottok løsning for $BEHOV for søknad $søknadId innsendt av $ident" }

            søknadRepository.hent(søknadId)?.let {
                if (it.ident == ident) {
                    søknadRepository.markerSøknadSomJournalført(søknadId, journalpostId, journalførtTidspunkt)
                    logg.info { "Søknad $søknadId markert som journalført" }
                    sikkerLogg.info { "Søknad $søknadId innsendt av $ident markert som journaført med journalpostId $journalpostId" }
                } else {
                    logg.warn {
                        "Søknad $søknadId har ikke samme registrerte ident som ident i mottatt melding, meldingen behandles ikke."
                    }
                    sikkerLogg.warn {
                        "Søknad $søknadId har ikke samme registrerte ident (${it.ident}) som ident i mottatt melding ($ident), meldingen behandles ikke"
                    }
                }
            } ?: also {
                logg.warn { "Fant ikke søknad $søknadId" }
                sikkerLogg.warn { "Fant ikke søknad $søknadId innsendt av $ident" }
            }
        }
    }
}
