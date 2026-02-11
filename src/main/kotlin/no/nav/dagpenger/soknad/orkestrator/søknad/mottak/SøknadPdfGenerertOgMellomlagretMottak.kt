package no.nav.dagpenger.soknad.orkestrator.søknad.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
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
import no.nav.dagpenger.soknad.orkestrator.utils.erBoolean
import java.util.UUID

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
            val behovForJournalføringAvSøknadPdfOgVedlegg =
                BehovForJournalføringAvSøknadPdfOgVedlegg(
                    søknadId = søknadId,
                    ident = ident,
                    dokumentvarianter = packet["@løsning"][BEHOV].dokumentVarianter(søknadId = søknadId),
                    dokumenter = søknadService.opprettDokumenterFraDokumentasjonskrav(søknadId, ident),
                    skjemakode = finnSkjemaKode(ident, søknadId),
                )

            rapidsConnection.publish(ident, behovForJournalføringAvSøknadPdfOgVedlegg.asMessage().toJson())
            logg.info { "Publiserte melding om behov for journalføring av søknad-PDF og vedlegg for søknad $søknadId " }
            sikkerLogg.info {
                "Publiserte melding om behov for journalføring av søknad-PDF og vedlegg for søknad $søknadId innsendt av $ident"
            }
        }
    }

    private fun finnSkjemaKode(
        ident: String,
        søknadId: UUID,
    ): String {
        val permittert = erSøkerenPermittert(ident, søknadId)
        val gjenopptak = erSøknadGjenopptak(ident, søknadId)

        return when {
            permittert && gjenopptak -> "04-16.04"
            permittert && !gjenopptak -> "04-01.04"
            !permittert && gjenopptak -> "04-16.03"
            else -> "04-01.03"
        }
    }

    private fun erSøkerenPermittert(
        ident: String,
        søknadId: UUID,
    ): Boolean {
        val seksjonsSvar =
            try {
                seksjonService.hentSeksjonsvar(
                    søknadId,
                    ident,
                    "arbeidsforhold",
                )
            } catch (e: IllegalStateException) {
                logg.info { "Fant ikke seksjonsvar for arbeidsforhold med søknadId-en: $søknadId" }
                return false
            }

        objectMapper.readTree(seksjonsSvar).let { seksjonsJson ->
            seksjonsJson.findPath("registrerteArbeidsforhold")?.let {
                if (!it.isMissingOrNull()) {
                    return it.any { arbeidsforhold ->
                        arbeidsforhold["hvordanHarDetteArbeidsforholdetEndretSeg"].asText() == "jegErPermitert"
                    }
                }
            }
        }
        return false
    }

    private fun erSøknadGjenopptak(
        ident: String,
        søknadId: UUID,
    ): Boolean {
        val seksjonsvar =
            try {
                seksjonService.hentSeksjonsvar(
                    søknadId,
                    ident,
                    "din-situasjon",
                )
            } catch (e: IllegalStateException) {
                logg.info { "Fant ikke seksjonsvar for din-situasjon med søknadId-en: $søknadId" }
                return false
            }

        objectMapper.readTree(seksjonsvar).let { seksjonsJson ->
            val dagpengerFraDato = seksjonsJson.findPath("harDuMottattDagpengerFraNavILøpetAvDeSiste52Ukene")
            if (!dagpengerFraDato.isMissingOrNull()) {
                return dagpengerFraDato.erBoolean()
            }
        }
        return false
    }
}
