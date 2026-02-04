package no.nav.dagpenger.soknad.orkestrator.behov

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.withMDC
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.SøknadsdataBehovløser
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepositoryPostgres
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonRepository

class SøknadsdataBehovMottak(
    rapidsConnection: RapidsConnection,
    opplysningRepository: QuizOpplysningRepositoryPostgres,
    seksjonRepository: SeksjonRepository,
    søknadRepository: SøknadRepository,
) : River.PacketListener {
    val søknadsdataBehovløser =
        SøknadsdataBehovløser(
            rapidsConnection,
            opplysningRepository,
            søknadRepository,
            seksjonRepository,
            FellesBehovløserLøsninger(
                opplysningRepository,
                søknadRepository,
                seksjonRepository,
            ),
        )

    private companion object {
        private val logger = KotlinLogging.logger {}
        private val behovIdSkipSet =
            setOf(
                "93fe996c-2ead-4e67-b6dc-cac88cf16954",
            )
    }

    init {
        River(rapidsConnection)
            .apply {
                precondition {
                    it.requireAllOrAny("@behov", listOf(søknadsdataBehovløser.behov))
                    it.requireValue("@event_name", "behov")
                    it.forbid("@løsning")
                    it.requireKey("journalpostId")
                }
                validate {
                    it.requireKey("ident", "@behovId")
                }
            }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val behovId = packet["@behovId"].asText()

        withMDC(
            mapOf(
                "behovId" to behovId,
            ),
        ) {
            logger.info { "Mottok behov: ${packet.get("@behov")}" }

            if (behovIdSkipSet.contains(behovId)) {
                logger.info { "Mottok behov $behovId som ligger i behovIdSkipSet, ignorerer meldingen." }
                return@withMDC
            }

            packet.løsBehov()
        }
    }

    private fun JsonMessage.løsBehov() {
        try {
            søknadsdataBehovløser.løs(SøknadBehovmelding(this))
        } catch (e: IllegalArgumentException) {
            logger.error(e) { "Kunne ikke løse behov Søknadsdata. Ett eller flere argumenter var feil." }
        } catch (e: IllegalStateException) {
            logger.error(e) { "Kunne ikke løse behov Søknadsdata. Opplysningen finnes ikke." }
        }
    }
}
