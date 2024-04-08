package no.nav.dagpenger.soknad.orkestrator

import mu.KotlinLogging
import no.nav.dagpenger.soknad.orkestrator.PostgresDataSourceBuilder.dataSource
import no.nav.dagpenger.soknad.orkestrator.PostgresDataSourceBuilder.runMigration
import no.nav.dagpenger.soknad.orkestrator.config.apiKonfigurasjon
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.name

internal class ApplicationBuilder(configuration: Map<String, String>) : RapidsConnection.StatusListener {
    private companion object {
        private val logger = KotlinLogging.logger {}
    }

    private val rapidsConnection =
        RapidApplication.Builder(RapidApplication.RapidApplicationConfig.fromEnv(configuration))
            .withKtorModule {
                apiKonfigurasjon()
            }
            .build()

    init {
        rapidsConnection.register(this)
    }

    internal fun start() {
        rapidsConnection.start()
    }

    override fun onNotReady(rapidsConnection: RapidsConnection) {
        logger.info { "OnNotReady" }
    }

    override fun onReady(rapidsConnection: RapidsConnection) {
        logger.info { "OnReady" }
    }

    override fun onShutdown(rapidsConnection: RapidsConnection) {
        logger.info { "OnShutDown" }
    }

    override fun onShutdownSignal(rapidsConnection: RapidsConnection) {
        logger.info { "OnShutDownSignal" }
    }

    override fun onStartup(rapidsConnection: RapidsConnection) {
        logger.info { "Starter dp-soknad-orkestrator" }
        Database.connect(datasource = dataSource)
            .also {
                logger.info { "Koblet til database ${it.name}}" }
                runMigration()
            }

        /*SøknadMottak(
            rapidsConnection,
            SøknadService(rapidsConnection),
            OpplysningRepositoryPostgres(dataSource),
        )
        BehovMottak(
            rapidsConnection = rapidsConnection,
            behovLøserFactory = BehovløserFactory(rapidsConnection, OpplysningRepositoryPostgres(dataSource)),
        )*/
    }
}
