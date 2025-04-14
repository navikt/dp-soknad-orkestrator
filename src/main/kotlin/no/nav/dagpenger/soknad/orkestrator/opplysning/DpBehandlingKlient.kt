package no.nav.dagpenger.soknad.orkestrator.opplysning

import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.nav.dagpenger.oauth2.CachedOauth2Client
import no.nav.dagpenger.soknad.orkestrator.api.models.OppdatertBarnRequestDTO
import no.nav.dagpenger.soknad.orkestrator.utils.configureHttpClient

class DpBehandlingKlient(
    val azureAdKlient: CachedOauth2Client,
    val dpBehandlingBaseUrl: String,
    val dpBehandlingScope: String,
    val httpKlient: HttpClient = configureHttpClient(),
) {
    fun oppdaterBarnOpplysning(
        oppdatertBarnRequest: OppdatertBarnRequestDTO,
        dpBehandlingOpplysning: DpBehandlingOpplysning,
        token: String,
    ) {
        val behandlingId = oppdatertBarnRequest.behandlingId
        val opplysningId = oppdatertBarnRequest.opplysningId

        val oboToken = azureAdKlient.onBehalfOf(token, dpBehandlingScope).access_token

        sikkerlogg.info {
            "Oppdaterer barn i DP behandling. " +
                "BehandlingId: $behandlingId " +
                "OpplysningId: $opplysningId " +
                "Verdi: ${dpBehandlingOpplysning.verdi} " +
                "Begrunnelse: ${dpBehandlingOpplysning.begrunnelse}"
        }

        runBlocking {
            val response: HttpResponse =
                httpKlient.put("$dpBehandlingBaseUrl/behandling/$behandlingId/opplysning/$opplysningId") {
                    accept(ContentType.Application.Json)
                    header("Authorization", "Bearer $oboToken")
                    contentType(ContentType.Application.Json)
                    setBody(dpBehandlingOpplysning)
                }

            if (response.status != HttpStatusCode.OK) {
                throw IllegalStateException(
                    "Feil ved oppdatering av barn i DP behandling. " +
                        "Statuskode: ${response.status} " +
                        "BehandlingId: $behandlingId " +
                        "OpplysningId: $opplysningId",
                )
            }
        }
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
        private val sikkerlogg = KotlinLogging.logger("tjenestekall.JournalføringService")
    }
}

data class DpBehandlingOpplysning(
    val verdi: String,
    val begrunnelse: String,
)
