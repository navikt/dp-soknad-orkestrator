package no.nav.dagpenger.soknad.orkestrator.opplysning

import com.fasterxml.jackson.annotation.JsonInclude
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.serialization.jackson3.jackson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.dagpenger.soknad.orkestrator.Configuration
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.introspect.DefaultAccessorNamingStrategy
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.module.SimpleModule
import tools.jackson.databind.ser.std.ToStringSerializer
import tools.jackson.module.kotlin.jacksonMapperBuilder
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue
import java.math.BigDecimal
import java.time.Duration

class EnhetsregisterKlient(
    private val enhetsregisterUrl: String = Configuration.enhetsregisteretUrl,
    val httpKlient: HttpClient = httpClient(),
) {
    private val mapper = jacksonObjectMapper()

    internal suspend fun hentOrganisasjon(organisasjonsNummer: String): String {
        runCatching {
            hentEnhet(organisasjonsNummer)
        }.onFailure {
            logger.error(it) { "Feil ved henting av organisasjonsnavn for $it" }
        }.onSuccess {
            val organisasjon = mapper.readValue<Organisasjon>(it)
            return organisasjon.navn
        }

        return ""
    }

    suspend fun hentEnhet(orgnummer: String): String =
        withContext(Dispatchers.IO) {
            try {
                httpKlient.get("$enhetsregisterUrl/api/enheter/$orgnummer").body()
            } catch (e: ClientRequestException) {
                when (e.response.status.value) {
                    404 -> httpKlient.get("$enhetsregisterUrl/api/underenheter/$orgnummer").body()
                    else -> throw e
                }
            }
        }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}

data class Organisasjon(
    val organisasjonsnummer: String,
    val navn: String,
)

internal fun httpClient(engine: HttpClientEngine = CIO.create { }): HttpClient =
    HttpClient(engine) {
        expectSuccess = true
        install(HttpTimeout) {
            connectTimeoutMillis = Duration.ofSeconds(5).toMillis()
            requestTimeoutMillis = Duration.ofSeconds(15).toMillis()
            socketTimeoutMillis = Duration.ofSeconds(15).toMillis()
        }

        install(ContentNegotiation) {
            jackson { inntektObjectMapper }
        }

        install(Logging) {
            level = LogLevel.INFO
        }
    }

val inntektObjectMapper: JsonMapper =
    jacksonMapperBuilder()
        .changeDefaultPropertyInclusion { it.withValueInclusion(JsonInclude.Include.NON_NULL) }
        // Bugfix: Dropper felter som begynner på Æ/Ø/Å
        .accessorNaming(DefaultAccessorNamingStrategy.Provider().withFirstCharAcceptance(true, true))
        .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
        .addModule(SimpleModule().addSerializer(BigDecimal::class.java, ToStringSerializer(BigDecimal::class.java)))
        .build()
