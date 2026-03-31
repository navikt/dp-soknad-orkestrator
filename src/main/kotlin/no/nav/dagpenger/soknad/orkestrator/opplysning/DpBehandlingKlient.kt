package no.nav.dagpenger.soknad.orkestrator.opplysning

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.oauth2.CachedOauth2Client
import no.nav.dagpenger.soknad.orkestrator.utils.configureHttpClient
import java.time.LocalDate
import java.util.UUID

class DpBehandlingKlient(
    val azureAdKlient: CachedOauth2Client,
    val dpBehandlingBaseUrl: String,
    val dpBehandlingScope: String,
    val httpKlient: HttpClient = configureHttpClient(),
) {
    fun oppdaterBarnOpplysning(
        behandlingId: UUID,
        dpBehandlingOpplysning: NyOpplysningDTO,
        token: String,
    ) {
        val oboToken = azureAdKlient.onBehalfOf(token, dpBehandlingScope).access_token

        sikkerlogg.info {
            "Oppdaterer barn i DP behandling. " +
                "BehandlingId: $behandlingId " +
                "Verdi: ${dpBehandlingOpplysning.verdi} " +
                "Begrunnelse: ${dpBehandlingOpplysning.begrunnelse}"
        }

        runBlocking {
            val response: HttpResponse =
                httpKlient.post("$dpBehandlingBaseUrl/behandling/$behandlingId/opplysning/") {
                    accept(ContentType.Application.Json)
                    header("Authorization", "Bearer $oboToken")
                    contentType(ContentType.Application.Json)
                    setBody(dpBehandlingOpplysning)
                }

            if (response.status != HttpStatusCode.OK) {
                throw IllegalStateException(
                    "Feil ved oppdatering av barn i DP behandling. " +
                        "Statuskode: ${response.status} " +
                        "BehandlingId: $behandlingId",
                )
            }
        }
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
        private val sikkerlogg = KotlinLogging.logger("tjenestekall.DpBehandlingKlient")
    }
}

private val BARN_OPPLYSNINGSTYPE_ID: UUID = UUID.fromString("0194881f-9428-74d5-b160-f63a4c61a23b")

data class NyOpplysningDTO(
    val opplysningstype: UUID = BARN_OPPLYSNINGSTYPE_ID,
    val verdi: String,
    val begrunnelse: String,
    val gyldigFraOgMed: LocalDate? = null,
    val gyldigTilOgMed: LocalDate? = null,
)
