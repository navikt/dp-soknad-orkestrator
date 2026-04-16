package no.nav.dagpenger.soknad.orkestrator.søknad.jobb

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.soknad.orkestrator.søknad.Tilstand
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.melding.MeldingOmSøknadKlarTilJournalføring
import no.nav.dagpenger.soknad.orkestrator.utils.NaisUtils
import no.nav.dagpenger.soknad.orkestrator.utils.defaultHttpClient
import java.util.UUID
import kotlin.concurrent.thread

internal object RekjørJournalføringForSøknaderJobb {
    private val logger = KotlinLogging.logger {}
    private val sikkerlogg = KotlinLogging.logger("tjenestekall.RekjørJournalføringForSøknaderJobb")
    private const val JOBBNAVN = "Jobb for rekjøre journalføring for søknader"

    private val søknadIderForMislykkedeSøknader: List<UUID> =
        listOf(
            UUID.fromString("fb774432-3801-4737-8914-fd8af550ff3e"), // dev
            UUID.fromString("1eb49db5-8387-4946-9b32-9f9d24caa944"),
            UUID.fromString("34a2c4aa-4b2e-49d0-98a7-09dffb806291"),
            UUID.fromString("5a2ada17-e1c0-473a-880d-ccaafc637a6f"),
            UUID.fromString("e46f72c3-a490-4bab-a29b-e1bf2705bbb5"),
            UUID.fromString("25d09010-4e2d-4ce0-bc1e-98917ad84085"),
            UUID.fromString("00a350d6-c3e1-4a8f-b756-6714be1cb089"),
        )

    fun startEngangsJobb(
        rapidsConnection: RapidsConnection,
        søknadRepository: SøknadRepository,
    ) {
        thread(name = JOBBNAVN, isDaemon = true) {
            try {
                if (NaisUtils().isLeader(defaultHttpClient)) {
                    logger.info { "Pod ${System.getenv("HOSTNAME")} er leader, starter \"$JOBBNAVN\" på denne podden." }
                    kjørJobb(rapidsConnection, søknadRepository, søknadIderForMislykkedeSøknader)
                } else {
                    logger.info { "Pod ${System.getenv("HOSTNAME")} er ikke leader, starter ikke \"$JOBBNAVN\" på denne podden" }
                }
            } catch (e: Exception) {
                logger.error(e) { "Kjøring av \"$JOBBNAVN\" feilet" }
            }
        }
    }

    internal fun kjørJobb(
        rapidsConnection: RapidsConnection,
        søknadRepository: SøknadRepository,
        søknadIder: List<UUID>,
    ) {
        logger.info { "Starter $JOBBNAVN for ${søknadIder.size} søknader" }

        søknadIder.forEach { søknadId ->
            try {
                val søknad = søknadRepository.hent(søknadId)

                if (søknad == null) {
                    logger.warn { "Fant ikke søknad med søknadId: $søknadId" }
                    return@forEach
                }

                if (søknad.tilstand == Tilstand.JOURNALFØRT) {
                    logger.warn { "Søknaden: $søknadId er allerede journalført" }
                    return@forEach
                }

                val melding = MeldingOmSøknadKlarTilJournalføring(søknadId, søknad.ident)
                rapidsConnection.publish(søknad.ident, melding.asMessage().toJson())

                logger.info { "Publiserte melding om søknad klar til journalføring med søknadId: $søknadId" }
                sikkerlogg.info {
                    "Publiserte melding om søknad klar til journalføring med søknadId: $søknadId og ident: ${søknad.ident}"
                }
            } catch (e: Exception) {
                logger.error(e) { "Feilet ved sending av melding for søknadId: $søknadId" }
            }
        }

        logger.info { "Fullførte $JOBBNAVN" }
    }
}
