package no.nav.dagpenger.soknad.orkestrator.opplysning

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.URLBuilder
import io.ktor.http.appendEncodedPathSegments
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.dagpenger.soknad.orkestrator.Configuration
import no.nav.dagpenger.soknad.orkestrator.utils.configureHttpClient
import java.time.LocalDate

internal class AaregClient(
    private val aaregUrl: String = Configuration.aaregUrl,
    private val tokenProvider: (String) -> String,
    val httpKlient: HttpClient = configureHttpClient(),
) {
    suspend fun hentArbeidsforhold(
        fnr: String,
        token: String,
    ) = withContext(Dispatchers.IO) {
        val urlBuilder = URLBuilder(aaregUrl).appendEncodedPathSegments(API_PATH, ARBEIDSFORHOLD_PATH).build()
        try {
            val response: HttpResponse =
                httpKlient.get(urlBuilder) {
                    header("Authorization", "Bearer ${tokenProvider.invoke(token)}")
                    header("Nav-Personident", fnr)
                    parameter("arbeidsforholdstatus", "AKTIV, AVSLUTTET")
                    parameter("historikk", "true")
                }
            if (response.status.value == 200) {
                logger.info { "Kall til AAREG gikk OK" }
                val arbeidsforholdJson = jacksonObjectMapper().readTree(response.bodyAsText())
                arbeidsforholdJson.map { toArbeidsforhold(it) }

                // Map the JSON to your data class here
                // Example: jacksonObjectMapper().readValue(arbeidsforholdJson, Array<Arbeidsforhold>::class.java).toList()
            } else {
                logger.warn { "Kall til AAREG feilet med status ${response.status}" }
                emptyList<String>()
            }
        } catch (e: Exception) {
            logger.warn { "Henting eller mapping av arbeidsforhold fra AAREG feilet: " + e }
            emptyList<String>()
        }
    }

    companion object {
        private const val API_PATH = "api"
        private const val ARBEIDSFORHOLD_PATH = "v2/arbeidstaker/arbeidsforhold"
        private val logger = KotlinLogging.logger {}
    }
}

data class ArbeidsforholdResponse(
    @get:JsonProperty("id")
    val id: kotlin.String,
    @get:JsonProperty("startdato")
    val startdato: java.time.LocalDate,
    @get:JsonProperty("sluttdato")
    val sluttdato: java.time.LocalDate? = null,
    @get:JsonProperty("organisasjonsnavn")
    val organisasjonsnavn: kotlin.String? = null,
)

internal data class Arbeidsforhold(
    val id: String,
    val organisasjonsnummer: String?,
    val startdato: LocalDate,
    val sluttdato: LocalDate?,
) {
    internal fun toResponse(organisasjonsnavn: String?) =
        ArbeidsforholdResponse(
            id = id,
            startdato = startdato,
            sluttdato = sluttdato,
            organisasjonsnavn = organisasjonsnavn,
        )
}

private fun toArbeidsforhold(aaregArbeidsforhold: JsonNode): Arbeidsforhold =
    Arbeidsforhold(
        id = aaregArbeidsforhold["navArbeidsforholdId"].asText(),
        organisasjonsnummer = toOrganisasjonsnummer(aaregArbeidsforhold["arbeidssted"]),
        startdato = aaregArbeidsforhold["ansettelsesperiode"]["startdato"].asLocalDate(),
        sluttdato = aaregArbeidsforhold["ansettelsesperiode"]["sluttdato"].asLocalDate(),
    )

private fun JsonNode?.asLocalDate(): LocalDate =
    this?.asText()?.let { LocalDate.parse(it) } ?: throw IllegalArgumentException("Dato kan ikke være null")

private fun toOrganisasjonsnummer(arbeidssted: JsonNode): String? =
    arbeidssted["identer"]
        .firstOrNull { it["type"].asText() == "ORGANISASJONSNUMMER" }
        ?.get("ident")
        ?.asText()
