package no.nav.dagpenger.soknad.orkestrator

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import mu.KotlinLogging
import no.nav.dagpenger.soknad.orkestrator.PostgresDataSourceBuilder.dataSource
import no.nav.dagpenger.soknad.orkestrator.PostgresDataSourceBuilder.runMigration
import no.nav.dagpenger.soknad.orkestrator.api.auth.AuthFactory.azureAd
import no.nav.dagpenger.soknad.orkestrator.api.auth.AuthFactory.tokenX
import no.nav.dagpenger.soknad.orkestrator.behov.BehovMottak
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory
import no.nav.dagpenger.soknad.orkestrator.config.configure
import no.nav.dagpenger.soknad.orkestrator.journalføring.JournalføringService
import no.nav.dagpenger.soknad.orkestrator.journalføring.MinidialogJournalførtMottak
import no.nav.dagpenger.soknad.orkestrator.opplysning.DpBehandlingKlient
import no.nav.dagpenger.soknad.orkestrator.opplysning.OpplysningService
import no.nav.dagpenger.soknad.orkestrator.opplysning.db.OpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.opplysning.landApi
import no.nav.dagpenger.soknad.orkestrator.opplysning.opplysningApi
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepositoryPostgres
import no.nav.dagpenger.soknad.orkestrator.søknad.SøknadMottak
import no.nav.dagpenger.soknad.orkestrator.søknad.SøknadService
import no.nav.dagpenger.soknad.orkestrator.søknad.SøknadSlettetMottak
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonService
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.seksjonApi
import no.nav.dagpenger.soknad.orkestrator.søknad.søknadApi
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

    private val seksjonRepository = SeksjonRepository(dataSource, søknadRepository)
    private val seksjonService = SeksjonService(seksjonRepository)

    private val søknadService: SøknadService =
        SøknadService(
            søknadRepository = søknadRepository,
            opplysningRepository = opplysningRepository,
        )

    private val journalføringService = JournalføringService()

    private val dpBehandlingKlient =
        DpBehandlingKlient(
            azureAdKlient = Configuration.azureAdClient,
            dpBehandlingBaseUrl = Configuration.miljøVariabler.dpBehandlingBaseUrl,
            dpBehandlingScope = Configuration.miljøVariabler.dpBehandlingScope,
        )

    private val opplysningService: OpplysningService =
        OpplysningService(
            opplysningRepository = quizOpplysningRepositoryPostgres,
            dpBehandlingKlient = dpBehandlingKlient,
        )

    private val rapidsConnection =
        RapidApplication
            .create(
                configuration,
                builder = {
                    withKtorModule {
                        install(ContentNegotiation) {
                            jackson { configure() }
                        }
                        install(Authentication) {
                            jwt("azureAd") {
                                azureAd()
                            }
                            jwt("tokenX") {
                                tokenX()
                            }
                        }
                        opplysningApi(opplysningService)
                        søknadApi(søknadService)
                        seksjonApi(seksjonService)
                        landApi()
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
