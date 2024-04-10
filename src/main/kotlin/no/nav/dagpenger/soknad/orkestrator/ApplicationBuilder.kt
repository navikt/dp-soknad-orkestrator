package no.nav.dagpenger.soknad.orkestrator

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import mu.KotlinLogging
import no.nav.dagpenger.soknad.orkestrator.PostgresDataSourceBuilder.clean
import no.nav.dagpenger.soknad.orkestrator.PostgresDataSourceBuilder.dataSource
import no.nav.dagpenger.soknad.orkestrator.PostgresDataSourceBuilder.runMigration
import no.nav.dagpenger.soknad.orkestrator.behov.BehovMottak
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory
import no.nav.dagpenger.soknad.orkestrator.config.apiKonfigurasjon
import no.nav.dagpenger.soknad.orkestrator.opplysning.db.OpplysningRepositoryPostgres
import no.nav.dagpenger.soknad.orkestrator.søknad.SøknadMottak
import no.nav.dagpenger.soknad.orkestrator.søknad.SøknadService
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
                routing {
                    get("/") { call.respond(HttpStatusCode.OK) }
                }
            }
            .build()

    init {
        rapidsConnection.register(this)
    }

    internal fun start() {
        rapidsConnection.start()
    }

    override fun onStartup(rapidsConnection: RapidsConnection) {
        logger.info { "Starter dp-soknad-orkestrator" }
        clean()
        Database.connect(datasource = dataSource)
            .also {
                logger.info { "Koblet til database ${it.name}}" }
                runMigration()
            }

        SøknadMottak(
            rapidsConnection,
            SøknadService(rapidsConnection),
            OpplysningRepositoryPostgres(dataSource),
        )
        BehovMottak(
            rapidsConnection = rapidsConnection,
            behovløserFactory = BehovløserFactory(rapidsConnection, OpplysningRepositoryPostgres(dataSource)),
        )
    }
}
