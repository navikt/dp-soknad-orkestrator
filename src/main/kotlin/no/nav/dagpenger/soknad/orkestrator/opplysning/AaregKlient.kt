package no.nav.dagpenger.soknad.orkestrator.opplysning

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.appendEncodedPathSegments
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.dagpenger.soknad.orkestrator.Configuration
import no.nav.dagpenger.soknad.orkestrator.utils.configureHttpClient

class AaregKlient(
    private val aaregUrl: String = Configuration.aaregUrl,
    private val tokenProvider: (String) -> String,
    val httpKlient: HttpClient = configureHttpClient(),
) {
    suspend fun hentArbeidsforhold(
        fnr: String,
        token: String,
    ): String =
        withContext(Dispatchers.IO) {
            val urlBuilder = URLBuilder(aaregUrl).appendEncodedPathSegments(API_PATH, ARBEIDSFORHOLD_PATH).build()
            val tokenTilAareg = tokenProvider.invoke(token)

            try {
                val response: HttpResponse =
                    httpKlient.post(urlBuilder) {
                        header("Authorization", "Bearer $tokenTilAareg")
                        contentType(ContentType.Application.Json)
                        setBody(ArbeidsforholdRequest(arbeidstakerId = fnr))
                    }

                if (response.status != HttpStatusCode.OK) {
                    logger.warn { "Kall til AAREG feilet med status ${response.status}" }
                    return@withContext ""
                }

                return@withContext response.bodyAsText()
            } catch (e: Exception) {
                logger.warn { "Henting eller mapping av arbeidsforhold fra AAREG feilet: " + e }
                ""
            }
        }

    companion object {
        private const val API_PATH = "api"
        private const val ARBEIDSFORHOLD_PATH = "v2/arbeidstaker/arbeidsforhold"
        private val logger = KotlinLogging.logger {}
    }
}

internal data class ArbeidsforholdRequest(
    val arbeidstakerId: String,
    val historikk: Boolean = true,
    val rapporteringsordninger: List<String> = listOf("A_ORDNINGEN"),
    val arbeidsforholdstatuser: List<String> = listOf("AKTIV", "AVSLUTTET"),
    val arbeidsforholdtyper: List<String> =
        listOf(
            "ordinaertArbeidsforhold",
            "maritimtArbeidsforhold",
            "forenkletOppgjoersordning",
            "frilanserOppdragstakerHonorarPersonerMm",
        ),
)
