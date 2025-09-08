package no.nav.dagpenger.soknad.orkestrator.personalia

import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
import com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel.INFO
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders.Accept
import io.ktor.http.HttpHeaders.Authorization
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.serialization.jackson.jackson

private val logger = KotlinLogging.logger {}

class KontonummerService(
    private val kontoRegisterUrl: String,
    private val tokenProvider: (subjectToken: String) -> String,
    httpClientEngine: HttpClientEngine = CIO.create(),
) {
    private val kontoNummberClient =
        HttpClient(httpClientEngine) {
            expectSuccess = true
            install(Logging) {
                level = INFO
            }
            install(ContentNegotiation) {
                jackson {
                    registerModule(JavaTimeModule())
                    configure(FAIL_ON_UNKNOWN_PROPERTIES, false)
                    disable(WRITE_DATES_AS_TIMESTAMPS)
                }
            }
        }

    suspend fun hentKontonummer(subjectToken: String): KontonummerDto =
        try {
            kontoNummberClient
                .get(kontoRegisterUrl) {
                    header(Authorization, "Bearer ${tokenProvider.invoke(subjectToken)}")
                    header(Accept, "application/json")
                }.body<KontoNummerRespsonse>()
                .let { map(it) }
        } catch (e: ClientRequestException) {
            if (e.response.status == NotFound) {
                logger.warn("Kontonummer ikke funnet for bruker")
            } else {
                logger.warn("Kall mot $kontoRegisterUrl feilet med klientfeil \"${e.response.status} ${e.response.bodyAsText()}\".", e)
            }
            KontonummerDto.TOM
        }

    private fun map(response: KontoNummerRespsonse): KontonummerDto =
        if (!response.kontonummer.isNullOrBlank()) {
            KontonummerDto(kontonummer = response.kontonummer)
        } else if (response.utenlandskKontoInfo != null) {
            KontonummerDto(
                kontonummer = response.utenlandskKontoInfo.bankkode,
                banknavn = response.utenlandskKontoInfo.banknavn,
                bankLandkode = response.utenlandskKontoInfo.bankLandkode,
            )
        } else {
            KontonummerDto.TOM
        }

    private data class KontoNummerRespsonse(
        val kontonummer: String? = null,
        val utenlandskKontoInfo: UtenlandskKontoInfoResponse? = null,
    ) {
        data class UtenlandskKontoInfoResponse(
            val banknavn: String? = null,
            val bankkode: String? = null,
            val bankLandkode: String? = null,
        )
    }
}
