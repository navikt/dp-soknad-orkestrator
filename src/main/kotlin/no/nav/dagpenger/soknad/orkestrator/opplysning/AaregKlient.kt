package no.nav.dagpenger.soknad.orkestrator.opplysning

import com.fasterxml.jackson.annotation.JsonProperty
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.URLBuilder
import io.ktor.http.appendEncodedPathSegments
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.dagpenger.soknad.orkestrator.Configuration
import no.nav.dagpenger.soknad.orkestrator.utils.configureHttpClient
import tools.jackson.databind.JsonNode
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDate

internal class AaregKlient(
    private val aaregUrl: String = Configuration.aaregUrl,
    private val tokenProvider: (String) -> String,
    val httpKlient: HttpClient = configureHttpClient(),
) {
    private val mapper = jacksonObjectMapper()

    suspend fun hentArbeidsforhold(
        fnr: String,
        token: String,
    ): List<Arbeidsforhold> =
        withContext(Dispatchers.IO) {
            val urlBuilder = URLBuilder(aaregUrl).appendEncodedPathSegments(API_PATH, ARBEIDSFORHOLD_PATH).build()
            logger.info { "aareg url: $urlBuilder" }
            logger.info { "token fra frontend: $token" }
            val tokenTilAareg = tokenProvider.invoke(token)
            logger.info { "tokenTilAareg: $tokenTilAareg" }

            try {
                val response: HttpResponse =
                    httpKlient.post(urlBuilder) {
                        header("Authorization", "Bearer $tokenTilAareg")
                        contentType(ContentType.Application.Json)
                        setBody(ArbeidsforholdRequest(arbeidstakerId = fnr))
                    }
                if (response.status.value == 200) {
                    logger.info { "Kall til AAREG gikk OK" }
                    val arbeidsforholdJson = mapper.readTree(response.bodyAsText()).values()
                    logger.info { arbeidsforholdJson.toString() }
                    arbeidsforholdJson.map(::toArbeidsforhold)
                } else {
                    logger.warn { "Kall til AAREG feilet med status ${response.status}" }
                    emptyList()
                }
            } catch (e: Exception) {
                logger.warn { "Henting eller mapping av arbeidsforhold fra AAREG feilet: " + e }
                emptyList()
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

data class ArbeidsforholdResponse(
    @get:JsonProperty("id")
    val id: kotlin.String,
    @get:JsonProperty("startdato")
    val startdato: java.time.LocalDate,
    @get:JsonProperty("sluttdato")
    val sluttdato: java.time.LocalDate? = null,
    @get:JsonProperty("sluttårsak")
    val sluttårsak: String? = null,
    @get:JsonProperty("arbeidstidsordning")
    val arbeidstidsordning: String,
    @get:JsonProperty("organisasjonsnavn")
    val organisasjonsnavn: kotlin.String? = null,
)

internal data class Permittering(
    val kode: String,
    val startdato: LocalDate,
    val prosent: Double,
)

internal data class Arbeidsforhold(
    val id: String,
    val organisasjonsnummer: String?,
    val organisasjonsnavn: String?,
    val startdato: LocalDate,
    val sluttdato: LocalDate?,
    val sluttårsak: String?,
    val arbeidstidsordning: String,
    val permitteringer: List<Permittering> = emptyList(),
) {
    internal fun toResponse(organisasjonsnavn: String?) =
        ArbeidsforholdResponse(
            id = id,
            startdato = startdato,
            sluttdato = sluttdato,
            sluttårsak = sluttårsak,
            arbeidstidsordning = arbeidstidsordning,
            organisasjonsnavn = organisasjonsnavn,
        )
}

private fun toArbeidsforhold(aaregArbeidsforhold: JsonNode): Arbeidsforhold =
    Arbeidsforhold(
        id = aaregArbeidsforhold["navArbeidsforholdId"].asString(),
        organisasjonsnummer = toOrganisasjonsnummer(aaregArbeidsforhold["arbeidssted"]),
        organisasjonsnavn = "",
        startdato = aaregArbeidsforhold["ansettelsesperiode"]["startdato"].asLocalDate(),
        sluttdato = aaregArbeidsforhold["ansettelsesperiode"]["sluttdato"].asNullableLocalDate(),
        sluttårsak =
            aaregArbeidsforhold["ansettelsesperiode"]
                .get("sluttaarsak")
                ?.get("kode")
                ?.asString(),
        arbeidstidsordning =
            aaregArbeidsforhold["ansettelsesdetaljer"]
                .firstOrNull()
                ?.get("arbeidstidsordning")
                ?.get("kode")
                ?.asString() ?: "Ukjent",
        permitteringer =
            aaregArbeidsforhold["permitteringer"]
                ?.values()
                ?.map {
                    Permittering(
                        kode = it["type"]["kode"].asString(),
                        startdato = it["startdato"].asLocalDate(),
                        prosent = it["prosent"].asDouble(),
                    )
                } ?: emptyList(),
    )

private fun JsonNode?.asLocalDate(): LocalDate =
    this?.asString()?.let { LocalDate.parse(it) } ?: throw IllegalArgumentException("Dato kan ikke være null")

private fun JsonNode?.asNullableLocalDate(): LocalDate? =
    this?.takeUnless { it.isNull || it.isMissingNode }?.asString()?.let(LocalDate::parse)

private fun toOrganisasjonsnummer(arbeidssted: JsonNode?): String? =
    arbeidssted
        ?.get("identer")
        ?.firstOrNull { it["type"].asString() == "ORGANISASJONSNUMMER" }
        ?.get("ident")
        ?.asString()
