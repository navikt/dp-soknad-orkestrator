package no.nav.dagpenger.soknad.orkestrator

import mu.KotlinLogging
import no.nav.dagpenger.soknad.orkestrator.PostgresDataSourceBuilder.dataSource
import no.nav.dagpenger.soknad.orkestrator.PostgresDataSourceBuilder.runMigration
import no.nav.dagpenger.soknad.orkestrator.opplysning.OpplysningRepositoryPostgres
import no.nav.dagpenger.soknad.orkestrator.søknad.SøknadMottak
import no.nav.dagpenger.soknad.orkestrator.søknad.SøknadService
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.name

internal class ApplicationBuilder(configuration: Map<String, String>) : RapidsConnection.StatusListener {
    private val logger = KotlinLogging.logger {}

    private val rapidsConnection =
        RapidApplication.Builder(
            RapidApplication.RapidApplicationConfig.fromEnv(configuration),
        ).build()

    init {
        rapidsConnection.register(this)
    }

    internal fun start() {
        rapidsConnection.start()
    }

    override fun onStartup(rapidsConnection: RapidsConnection) {
        Database.connect(datasource = dataSource)
            .apply {
                logger.info { "Koblet til database $name}" }
                runMigration()
            }

        SøknadMottak(rapidsConnection, SøknadService(rapidsConnection), OpplysningRepositoryPostgres(dataSource))
    }
}
