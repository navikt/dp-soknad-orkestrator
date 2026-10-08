package no.nav.dagpenger.soknad.orkestrator.opplysning

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.dagpenger.soknad.orkestrator.Configuration
import no.nav.dagpenger.soknad.orkestrator.utils.configureHttpClient
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue

class EnhetsregisterKlient(
    private val enhetsregisterUrl: String = Configuration.enhetsregisteretUrl,
    val httpKlient: HttpClient = configureHttpClient(),
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
