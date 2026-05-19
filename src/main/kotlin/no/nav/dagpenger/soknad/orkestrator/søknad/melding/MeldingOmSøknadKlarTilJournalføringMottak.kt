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
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonRepository
import no.nav.dagpenger.soknad.orkestrator.utils.asUUID
import java.util.UUID

class MeldingOmSøknadKlarTilJournalføringMottak(
    private val rapidsConnection: RapidsConnection,
    private val søknadRepository: SøknadRepository,
    private val seksjonRepository: SeksjonRepository,
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
                val søknadIderForMislykkedeSøknader: List<UUID> =
                    listOf(
                        UUID.fromString("8feb4a0a-2346-44b9-a3e2-e438d05598b8"), // dev
                        UUID.fromString("1eb49db5-8387-4946-9b32-9f9d24caa944"),
                        UUID.fromString("34a2c4aa-4b2e-49d0-98a7-09dffb806291"),
                        UUID.fromString("5a2ada17-e1c0-473a-880d-ccaafc637a6f"),
                        UUID.fromString("e46f72c3-a490-4bab-a29b-e1bf2705bbb5"),
                        UUID.fromString("25d09010-4e2d-4ce0-bc1e-98917ad84085"),
                        UUID.fromString("00a350d6-c3e1-4a8f-b756-6714be1cb089"),
                    )

                søknadRepository
                    .hent(søknadId)
                    ?.let {
                        if (it.tilstand != PÅBEGYNT && it.søknadId !in søknadIderForMislykkedeSøknader) {
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

                        if (it.søknadId !in søknadIderForMislykkedeSøknader) {
                            søknadRepository.markerSøknadSomInnsendt(søknadId, ident, innsendtTidspunkt)
                        }

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
                        var seksjonsdata = seksjonRepository.hentSeksjonForStatistikk(søknadId, ident)
                        var pdfGrunnlag = seksjonRepository.hentAllePdfGrunnlag(søknadId, ident)

                        val søknadEndretTilstandMelding =
                            SøknadEndretTilstandMelding(
                                søknadId = søknadId,
                                ident = ident,
                                forrigeTilstand = PÅBEGYNT.name,
                                nyTilstand = Tilstand.INNSENDT.name,
                                søknad = it,
                                søknadsdata = seksjonsdata,
                                pdfGrunnlag = pdfGrunnlag,
                            )
                        rapidsConnection.publish(
                            ident,
                            søknadEndretTilstandMelding.asMessage().toJson(),
                        )
                        logg.info { "Publiserte endret tilstand til Innsendt melding for $søknadId" }
                        sikkerLogg.info {
                            "Publiserte endret tilstand til Innsendt melding for $søknadId innsendt av $ident. Melding: ${søknadEndretTilstandMelding.asMessage().toJson()}"
                        }

                        val dokumentasjonsKravLister = seksjonRepository.hentDokumentasjonskrav(søknadId, ident)
                        val dokumentasjonsKravInnsendtMelding =
                            DokumentKravInnsendtMelding(
                                søknadId = søknadId,
                                ident = ident,
                                dokumenter = dokumentasjonsKravLister,
                                innsendtTidspunkt = innsendtTidspunkt,
                                tilstand = Tilstand.INNSENDT,
                            )
                        rapidsConnection.publish(
                            ident,
                            dokumentasjonsKravInnsendtMelding.asMessage().toJson(),
                        )
                        logg.info { "Publiserte melding om dokumentasjonskrav for søknad $søknadId" }
                        sikkerLogg.info {
                            "Publiserte melding om dokumentasjonskrav for søknad $søknadId innsendt av $ident: ${dokumentasjonsKravInnsendtMelding.asMessage().toJson()}"
                        }
                    } ?: also {
                    logg.warn { "Fant ikke søknad $søknadId" }
                    sikkerLogg.warn { "Fant ikke søknad $søknadId innsendt av $ident" }
                    return@withMDC
                }
            } catch (exception: Exception) {
                logg.info { "Mottak av $EVENT_NAME hendelse for søknad ${packet["søknadId"]} feilet" }
                sikkerLogg.info { "Mottak av $EVENT_NAME hendelse for søknad ${packet["søknadId"]} og ident ${packet["ident"]} feilet" }
                throw exception
            }
        }
    }
}
