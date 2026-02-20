package no.nav.dagpenger.soknad.orkestrator.søknad.melding

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers.withMDC
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import no.nav.dagpenger.soknad.orkestrator.søknad.Tilstand
import no.nav.dagpenger.soknad.orkestrator.søknad.Tilstand.PÅBEGYNT
import no.nav.dagpenger.soknad.orkestrator.søknad.behov.BehovForGenereringOgMellomlagringAvSøknadPdf
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.pdf.PdfPayloadService
import no.nav.dagpenger.soknad.orkestrator.utils.asUUID

class MeldingOmSøknadKlarTilJournalføringMottak(
    private val rapidsConnection: RapidsConnection,
    private val søknadRepository: SøknadRepository,
    private val pdfPayloadService: PdfPayloadService,
) : River.PacketListener {
    companion object {
        private val logg = KotlinLogging.logger {}
        private val sikkerLogg = KotlinLogging.logger("tjenestekall.${this::class.simpleName}")
        const val EVENT_NAME = "søknad_klar_til_journalføring"
    }

    init {
        River(rapidsConnection)
            .apply {
                precondition {
                    it.requireValue("@event_name", EVENT_NAME)
                }
                validate {
                    it.requireKey("ident", "søknadId", "innsendtTidspunkt")
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
            logg.info { "Mottok $EVENT_NAME hendelse for søknad ${packet["søknadId"]}" }
            sikkerLogg.info { "Mottok $EVENT_NAME hendelse for søknad ${packet["søknadId"]} og ident ${packet["ident"]}" }

            try {
                val jsonNode = objectMapper.readTree(packet.toJson())
                val ident = jsonNode.get("ident").asText()
                val søknadId = jsonNode.get("søknadId").asUUID()
                val innsendtTidspunkt = jsonNode.get("innsendtTidspunkt").asLocalDateTime()

                søknadRepository
                    .hent(søknadId)
                    ?.let {
                        if (it.tilstand != PÅBEGYNT) {
                            logg.warn {
                                "Søknad $søknadId må ha tilstand $PÅBEGYNT for å kunne sendes inn, men har tilstand ${it.tilstand}, meldingen behandles ikke"
                            }
                            sikkerLogg.warn {
                                "Søknad $søknadId som tilhører $ident må ha tilstand $PÅBEGYNT for å kunne sendes inn, men har tilstand ${it.tilstand}, meldingen behandles ikke"
                            }
                            return@withMDC
                        }

                        if (it.ident != ident) {
                            logg.warn {
                                "Søknad $søknadId har ikke samme registrerte ident som ident i mottatt melding, meldingen behandles ikke."
                            }
                            sikkerLogg.warn {
                                "Søknad $søknadId har ikke samme registrerte ident (${it.ident}) som ident i mottatt melding ($ident), meldingen behandles ikke"
                            }
                            return@withMDC
                        }

                        søknadRepository.markerSøknadSomInnsendt(søknadId, ident, innsendtTidspunkt)
                        logg.info { "Søknad $søknadId markert som innsendt" }
                        sikkerLogg.info { "Søknad $søknadId innsendt av $ident markert som innsendt" }
                        rapidsConnection.publish(
                            ident,
                            BehovForGenereringOgMellomlagringAvSøknadPdf(
                                søknadId,
                                ident,
                                pdfPayloadService.genererBruttoPdfPayload(søknadId, ident),
                                pdfPayloadService.genererNettoPdfPayload(søknadId, ident),
                            ).asMessage().toJson(),
                        )
                        logg.info { "Publiserte melding om behov for generering av søknad-PDF for søknad $søknadId " }
                        sikkerLogg.info {
                            "Publiserte melding om behov for generering av søknad-PDF for søknad $søknadId innsendt av $ident "
                        }

                        val søknadEndretTilstandMelding =
                            SøknadEndretTilstandMelding(
                                søknadId = søknadId,
                                ident = ident,
                                forrigeTilstand = PÅBEGYNT.name,
                                nyTilstand = Tilstand.INNSENDT.name,
                            )
                        rapidsConnection.publish(
                            ident,
                            søknadEndretTilstandMelding.asMessage().toJson(),
                        )
                        logg.info { "Publiserte endret tilstand til Innsendt melding for $søknadId" }
                        sikkerLogg.info { "Publiserte endret tilstand til Innsendt melding for $søknadId innsendt av $ident" }
                    } ?: also {
                    logg.warn { "Fant ikke søknad $søknadId" }
                    sikkerLogg.warn { "Fant ikke søknad $søknadId innsendt av $ident" }
                }
            } catch (exception: Exception) {
                logg.info { "Mottak av $EVENT_NAME hendelse for søknad ${packet["søknadId"]} feilet" }
                sikkerLogg.info { "Mottak av $EVENT_NAME hendelse for søknad ${packet["søknadId"]} og ident ${packet["ident"]} feilet" }
                throw exception
            }
        }
    }
}
