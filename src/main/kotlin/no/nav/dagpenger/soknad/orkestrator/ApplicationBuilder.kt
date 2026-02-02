package no.nav.dagpenger.soknad.orkestrator

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import no.nav.dagpenger.pdl.createPersonOppslag
import no.nav.dagpenger.pdl.createPersonOppslagBolk
import no.nav.dagpenger.soknad.orkestrator.Configuration.azureAdClient
import no.nav.dagpenger.soknad.orkestrator.Configuration.tokenXClient
import no.nav.dagpenger.soknad.orkestrator.PostgresDataSourceBuilder.dataSource
import no.nav.dagpenger.soknad.orkestrator.PostgresDataSourceBuilder.runMigration
import no.nav.dagpenger.soknad.orkestrator.api.auth.AuthFactory.azureAd
import no.nav.dagpenger.soknad.orkestrator.api.auth.AuthFactory.tokenX
import no.nav.dagpenger.soknad.orkestrator.barn.BarnService
import no.nav.dagpenger.soknad.orkestrator.barn.barnApi
import no.nav.dagpenger.soknad.orkestrator.behov.BehovMottak
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory
import no.nav.dagpenger.soknad.orkestrator.behov.SøknadsdataBehovMottak
import no.nav.dagpenger.soknad.orkestrator.config.configure
import no.nav.dagpenger.soknad.orkestrator.journalføring.JournalføringService
import no.nav.dagpenger.soknad.orkestrator.journalføring.MinidialogJournalførtMottak
import no.nav.dagpenger.soknad.orkestrator.opplysning.DpBehandlingKlient
import no.nav.dagpenger.soknad.orkestrator.opplysning.OpplysningService
import no.nav.dagpenger.soknad.orkestrator.opplysning.landApi
import no.nav.dagpenger.soknad.orkestrator.opplysning.opplysningApi
import no.nav.dagpenger.soknad.orkestrator.personalia.KontonummerService
import no.nav.dagpenger.soknad.orkestrator.personalia.PersonService
import no.nav.dagpenger.soknad.orkestrator.personalia.PersonaliaService
import no.nav.dagpenger.soknad.orkestrator.personalia.personaliaApi
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepositoryPostgres
import no.nav.dagpenger.soknad.orkestrator.søknad.SøknadService
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadPersonaliaRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.jobb.SlettSøknaderSomErPåbegyntOgIkkeOppdatertPå7DagerJobb
import no.nav.dagpenger.soknad.orkestrator.søknad.melding.MeldingOmSøknadKlarTilJournalføringMottak
import no.nav.dagpenger.soknad.orkestrator.søknad.mottak.MeldingOmEttersendingMottak
import no.nav.dagpenger.soknad.orkestrator.søknad.mottak.SøknadMottak
import no.nav.dagpenger.soknad.orkestrator.søknad.mottak.SøknadPdfGenerertOgMellomlagretMottak
import no.nav.dagpenger.soknad.orkestrator.søknad.mottak.SøknadPdfOgVedleggJournalførtMottak
import no.nav.dagpenger.soknad.orkestrator.søknad.mottak.SøknadSlettetMottak
import no.nav.dagpenger.soknad.orkestrator.søknad.pdf.PdfPayloadService
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
    private val søknadPersonaliaRepository = SøknadPersonaliaRepository(dataSource)

    private val seksjonRepository = SeksjonRepository(dataSource, søknadRepository)
    private val seksjonService = SeksjonService(seksjonRepository, søknadRepository)
    private val personaliaService =
        PersonaliaService(
            personService =
                PersonService(
                    personOppslag = createPersonOppslag(url = Configuration.pdlApiUrl),
                    tokenProvider = tokenXClient(audience = Configuration.pdlApiUserScope),
                ),
            kontonummerService =
                KontonummerService(
                    kontoRegisterUrl = Configuration.personKontoRegisterUrl,
                    tokenProvider = tokenXClient(audience = Configuration.personKontoRegisterScope),
                ),
        )
    private val barnService =
        BarnService(
            personOppslagBolk = createPersonOppslagBolk(url = Configuration.pdlApiUrl),
            tokenProvider = {
                azureAdClient
                    .clientCredentials(
                        scope = Configuration.pdlApiSystemScope,
                    ).access_token ?: throw RuntimeException("Kunne ikke hente token")
            },
        )

    private val søknadService: SøknadService =
        SøknadService(
            søknadRepository = søknadRepository,
            søknadPersonaliaRepository = søknadPersonaliaRepository,
            seksjonRepository = seksjonRepository,
        )

    private val journalføringService = JournalføringService()

    private val dpBehandlingKlient =
        DpBehandlingKlient(
            azureAdKlient = azureAdClient,
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
                        søknadApi(søknadService, seksjonService)
                        seksjonApi(seksjonService)
                        personaliaApi(personaliaService)
                        barnApi(barnService)
                        landApi()
                    }
                },
            ).also { rapidsConnection ->
                søknadService.setRapidsConnection(rapidsConnection)
                seksjonService.setRapidsConnection(rapidsConnection)
                journalføringService.setRapidsConnection(rapidsConnection)
                SøknadMottak(rapidsConnection, søknadService, søknadRepository)
                MeldingOmSøknadKlarTilJournalføringMottak(
                    rapidsConnection,
                    søknadRepository,
                    PdfPayloadService(søknadRepository, søknadPersonaliaRepository, seksjonRepository),
                )
                SøknadPdfGenerertOgMellomlagretMottak(rapidsConnection, søknadService)
                SøknadPdfOgVedleggJournalførtMottak(rapidsConnection, søknadRepository)
                MinidialogJournalførtMottak(rapidsConnection)
                BehovMottak(
                    rapidsConnection = rapidsConnection,
                    behovløserFactory =
                        BehovløserFactory(
                            rapidsConnection,
                            QuizOpplysningRepositoryPostgres(dataSource),
                            seksjonRepository,
                            søknadRepository,
                        ),
                    søknadService = søknadService,
                )
                SøknadsdataBehovMottak(
                    rapidsConnection,
                    QuizOpplysningRepositoryPostgres(dataSource),
                    seksjonRepository,
                    søknadRepository,
                )
                SøknadSlettetMottak(rapidsConnection, søknadService)
                MeldingOmEttersendingMottak(rapidsConnection, søknadRepository, seksjonRepository)
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
        SlettSøknaderSomErPåbegyntOgIkkeOppdatertPå7DagerJobb.startFixedRateTimer(søknadService)
    }
}
