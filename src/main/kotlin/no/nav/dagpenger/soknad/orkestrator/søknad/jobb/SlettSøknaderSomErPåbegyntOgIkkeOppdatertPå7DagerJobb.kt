package no.nav.dagpenger.soknad.orkestrator.søknad.jobb

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.soknad.orkestrator.søknad.SøknadService
import no.nav.dagpenger.soknad.orkestrator.utils.NaisUtils
import no.nav.dagpenger.soknad.orkestrator.utils.defaultHttpClient
import kotlin.concurrent.fixedRateTimer
import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.time.Duration.Companion.minutes

internal object SlettSøknaderSomErPåbegyntOgIkkeOppdatertPå7DagerJobb {
    private val logger = KotlinLogging.logger {}
    private const val JOBBNAVN = "Slettejobb for søknader som er påbegynt og ikke oppdatert på 7 dager"

    fun startFixedRateTimer(søknadService: SøknadService) {
        fixedRateTimer(
            name = JOBBNAVN,
            daemon = true,
            initialDelay = Random.nextInt(1..10).minutes.inWholeMilliseconds,
            period = 15.minutes.inWholeMilliseconds,
            action = {
                try {
                    if (NaisUtils().isLeader(defaultHttpClient)) {
                        logger.info { "Pod ${System.getenv("HOSTNAME")} er leader, starter \"$JOBBNAVN\" på denne podden." }
                        søknadService.slettSøknaderSomErPåbegyntOgIkkeOppdatertPå7Dager()
                    } else {
                        logger.info { "Pod ${System.getenv("HOSTNAME")} er ikke leader, starter ikke \"$JOBBNAVN\" på denne podden" }
                    }
                } catch (e: Exception) {
                    logger.error(e) { "Kjøring av \"$JOBBNAVN\" feilet" }
                }
            },
        )
    }
}
