package no.nav.dagpenger.soknad.orkestrator

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import mu.KotlinLogging
import no.nav.dagpenger.soknad.orkestrator.PostgresDataSourceBuilder.dataSource
import no.nav.dagpenger.soknad.orkestrator.PostgresDataSourceBuilder.runMigration
import no.nav.dagpenger.soknad.orkestrator.api.internalApi
import no.nav.dagpenger.soknad.orkestrator.behov.BehovMottak
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory
import no.nav.dagpenger.soknad.orkestrator.config.apiKonfigurasjon
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepositoryPostgres
import no.nav.dagpenger.soknad.orkestrator.søknad.SøknadMottak
import no.nav.dagpenger.soknad.orkestrator.søknad.SøknadService
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.søknadApi
import no.nav.helse.rapids_rivers.RapidApplication
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.name

internal class ApplicationBuilder(configuration: Map<String, String>) : RapidsConnection.StatusListener {
    private companion object {
        private val logger = KotlinLogging.logger {}
    }

    private val rapidsConnection =
        RapidApplication.create(configuration) { engine, _ ->
            with(engine.application) {
                apiKonfigurasjon()
                internalApi()
                søknadApi(søknadService = søknadService())
            }
        }

    private val quizOpplysningRepositoryPostgres = QuizOpplysningRepositoryPostgres(dataSource)
    private val søknadRepository =
        SøknadRepository(
            dataSource = dataSource,
            quizOpplysningRepository = quizOpplysningRepositoryPostgres,
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
        Database.connect(datasource = dataSource)
            .also {
                logger.info { "Koblet til database ${it.name}}" }
                runMigration()
            }

        SøknadMottak(
            rapidsConnection,
            søknadService(),
            søknadRepository,
        )
        BehovMottak(
            rapidsConnection = rapidsConnection,
            behovløserFactory = BehovløserFactory(rapidsConnection, QuizOpplysningRepositoryPostgres(dataSource)),
            søknadService = søknadService(),
        )
    }

    private fun søknadService(): SøknadService = søknadService
}
