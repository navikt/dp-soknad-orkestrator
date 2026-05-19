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
            UUID.fromString("8feb4a0a-2346-44b9-a3e2-e438d05598b8"),
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
