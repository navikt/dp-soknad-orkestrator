package no.nav.dagpenger.soknad.orkestrator

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import mu.KotlinLogging
import no.nav.dagpenger.soknad.orkestrator.PostgresDataSourceBuilder.dataSource
import no.nav.dagpenger.soknad.orkestrator.PostgresDataSourceBuilder.runMigration
import no.nav.dagpenger.soknad.orkestrator.behov.BehovMottak
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import no.nav.dagpenger.soknad.orkestrator.inntekt.InntektService
import no.nav.dagpenger.soknad.orkestrator.inntekt.inntektApi
import no.nav.dagpenger.soknad.orkestrator.journalføring.JournalføringService
import no.nav.dagpenger.soknad.orkestrator.journalføring.MinidialogJournalførtMottak
import no.nav.dagpenger.soknad.orkestrator.opplysning.db.OpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.opplysning.landgruppeApi
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepositoryPostgres
import no.nav.dagpenger.soknad.orkestrator.søknad.SøknadMottak
import no.nav.dagpenger.soknad.orkestrator.søknad.SøknadService
import no.nav.dagpenger.soknad.orkestrator.søknad.SøknadSlettetMottak
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.helse.rapids_rivers.RapidApplication
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.name

internal class ApplicationBuilder(
    configuration: Map<String, String>,
) : RapidsConnection.StatusListener {
    private companion object {
        private val logger = KotlinLogging.logger {}
    }

    private val quizOpplysningRepositoryPostgres = QuizOpplysningRepositoryPostgres(dataSource)
    private val søknadRepository =
        SøknadRepository(
            dataSource = dataSource,
            quizOpplysningRepository = quizOpplysningRepositoryPostgres,
        )
    private val opplysningRepository = OpplysningRepository(dataSource)

    private val søknadService: SøknadService =
        SøknadService(
            søknadRepository = søknadRepository,
            opplysningRepository = opplysningRepository,
        )

    private val journalføringService = JournalføringService()

    private val inntektService: InntektService = InntektService(journalføringService)

    private val rapidsConnection =
        RapidApplication
            .create(
                configuration,
                builder = {
                    withKtorModule {

                        install(ContentNegotiation) {
                            jackson { objectMapper }
                        }

                        landgruppeApi()
                        inntektApi(inntektService)
                    }
                },
            ).also { rapidsConnection ->
                søknadService.setRapidsConnection(rapidsConnection)
                journalføringService.setRapidsConnection(rapidsConnection)
                SøknadMottak(rapidsConnection, søknadService, søknadRepository)
                MinidialogJournalførtMottak(rapidsConnection)
                BehovMottak(
                    rapidsConnection = rapidsConnection,
                    behovløserFactory =
                        BehovløserFactory(
                            rapidsConnection,
                            QuizOpplysningRepositoryPostgres(dataSource),
                        ),
                    søknadService = søknadService,
                )
                SøknadSlettetMottak(rapidsConnection, søknadService)
            }

    init {
        rapidsConnection.register(this)
    }

    internal fun start() {
        rapidsConnection.start()
    }

    override fun onStartup(rapidsConnection: RapidsConnection) {
        logger.info { "Starter dp-soknad-orkestrator" }
        Database
            .connect(datasource = dataSource)
            .also {
                logger.info { "Koblet til database ${it.name}}" }
                runMigration()
            }
    }
}
