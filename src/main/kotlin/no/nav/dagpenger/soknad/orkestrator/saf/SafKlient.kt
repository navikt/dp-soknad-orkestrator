package no.nav.dagpenger.soknad.orkestrator.saf

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.oauth2.CachedOauth2Client
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import no.nav.dagpenger.soknad.orkestrator.utils.configureHttpClient
import java.util.UUID

private val journalpostQuery =
    """
    query(${'$'}journalpostId: String!) {
        journalpost(journalpostId: ${'$'}journalpostId) {
            journalpostId
            dokumenter {
                tittel
                dokumentInfoId
                brevkode
            }
        }
    }
    """.trimIndent()

class SafKlient(
    private val azureAdClient: CachedOauth2Client,
    private val safUrl: String,
    private val safScope: String,
    private val httpClient: HttpClient = configureHttpClient(),
) {
    private fun token() =
        azureAdClient.clientCredentials(safScope).access_token
            ?: throw RuntimeException("Klarte ikke hente SAF-token")

    fun hentSøknadUuid(journalpostId: String): UUID? =
        runBlocking {
            try {
                logger.info { "Slår opp journalpost i SAF for journalpostId: $journalpostId" }
                val journalpost = hentJournalpost(journalpostId)
                val dokumentInfoId = journalpost.hovedDokument.dokumentInfoId

                sikkerlogg.info { "Henter søknadsdata fra SAF for journalpostId: $journalpostId, dokumentInfoId: $dokumentInfoId" }

                val søknadsData = hentSøknadsData(journalpostId, dokumentInfoId)
                sikkerlogg.info { "Søknadsdata fra SAF for journalpostId: $journalpostId: $søknadsData" }
                val søknadUuid =
                    søknadsData["søknad_uuid"]?.textValue()
                        ?: throw IllegalStateException(
                            "Fant ikke søknad_uuid i SAF-dokument for journalpostId: $journalpostId",
                        )

                UUID.fromString(søknadUuid)
            } catch (e: ClientRequestException) {
                if (e.response.status == HttpStatusCode.NotFound) {
                    logger.warn { "Fant ikke dokument i SAF for journalpostId: $journalpostId, svarer uten søknad_uuid" }
                    null
                } else {
                    logger.error(e) { "SAF-oppslag feilet for journalpostId: $journalpostId" }
                    throw e
                }
            } catch (e: Exception) {
                logger.error(e) { "SAF-oppslag feilet for journalpostId: $journalpostId" }
                throw e
            }
        }

    private suspend fun hentJournalpost(journalpostId: String): SafJournalpost {
        val body =
            mapOf(
                "query" to journalpostQuery,
                "variables" to mapOf("journalpostId" to journalpostId),
            )

        val json =
            httpClient
                .post("$safUrl/graphql") {
                    header(HttpHeaders.Authorization, "Bearer ${token()}")
                    contentType(ContentType.Application.Json)
                    accept(ContentType.Application.Json)
                    setBody(body)
                }.bodyAsText()

        return SafJournalpost.fraGraphQlJson(json)
    }

    private suspend fun hentSøknadsData(
        journalpostId: String,
        dokumentInfoId: String,
    ) = objectMapper.readTree(
        httpClient
            .get("$safUrl/rest/hentdokument/$journalpostId/$dokumentInfoId/ORIGINAL") {
                header(HttpHeaders.Authorization, "Bearer ${token()}")
            }.bodyAsText(),
    )

    private companion object {
        private val logger = KotlinLogging.logger {}
        private val sikkerlogg = KotlinLogging.logger("tjenestekall.SafKlient")
    }
}
