package no.nav.dagpenger.soknad.orkestrator

import mu.KotlinLogging
import no.nav.dagpenger.soknad.orkestrator.PostgresDataSourceBuilder.clean
import no.nav.dagpenger.soknad.orkestrator.PostgresDataSourceBuilder.dataSource
import no.nav.dagpenger.soknad.orkestrator.PostgresDataSourceBuilder.runMigration
import no.nav.dagpenger.soknad.orkestrator.api.internalApi
import no.nav.dagpenger.soknad.orkestrator.behov.BehovMottak
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory
import no.nav.dagpenger.soknad.orkestrator.config.Configuration.config
import no.nav.dagpenger.soknad.orkestrator.config.apiKonfigurasjon
import no.nav.dagpenger.soknad.orkestrator.opplysning.db.OpplysningRepositoryPostgres
import no.nav.dagpenger.soknad.orkestrator.søknad.SøknadMottak
import no.nav.dagpenger.soknad.orkestrator.søknad.SøknadService
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.søknadApi
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
                internalApi()
                søknadApi(
                    søknadService = søknadService(),
                )
            }
            .build()

    private val opplysningRepositoryPostgres = OpplysningRepositoryPostgres(dataSource)
    private val søknadRepository =
        SøknadRepository(
            dataSource = dataSource,
            opplysningRepository = opplysningRepositoryPostgres,
        )

    private val søknadService: SøknadService = SøknadService(rapid = rapidsConnection, søknadRepository = søknadRepository)

    init {
        rapidsConnection.register(this)
    }

    internal fun start() {
        rapidsConnection.start()
    }

    override fun onStartup(rapidsConnection: RapidsConnection) {
        logger.info { "Starter dp-soknad-orkestrator" }
        if (config["CLEAN_ON_STARTUP"] == "true") clean()
        Database.connect(datasource = dataSource)
            .also {
                logger.info { "Koblet til database ${it.name}}" }
                runMigration()
            }

        SøknadMottak(
            rapidsConnection,
            søknadService(),
            OpplysningRepositoryPostgres(dataSource),
        )
        BehovMottak(
            rapidsConnection = rapidsConnection,
            behovløserFactory = BehovløserFactory(rapidsConnection, OpplysningRepositoryPostgres(dataSource)),
        )
    }

    private fun søknadService(): SøknadService = søknadService
}
