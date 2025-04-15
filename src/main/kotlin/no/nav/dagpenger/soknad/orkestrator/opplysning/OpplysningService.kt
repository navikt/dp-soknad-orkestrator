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
import no.nav.dagpenger.soknad.orkestrator.api.models.BarnOpplysningDTO
import no.nav.dagpenger.soknad.orkestrator.api.models.BarnOpplysningDTO.DataType
import no.nav.dagpenger.soknad.orkestrator.api.models.BarnOpplysningDTO.Kilde
import no.nav.dagpenger.soknad.orkestrator.api.models.BarnResponseDTO
import no.nav.dagpenger.soknad.orkestrator.api.models.OppdatertBarnDTO
import no.nav.dagpenger.soknad.orkestrator.api.models.OppdatertBarnRequestDTO
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.BarnetilleggBehovLøser.Companion.beskrivendeIdEgneBarn
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.BarnetilleggBehovLøser.Companion.beskrivendeIdPdlBarn
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.BarnetilleggBehovLøser.Løsningsbarn
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.asListOf
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Barn
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.BarnSvar
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.utils.configureHttpClient
import java.util.UUID

class OpplysningService(
    val azureAdKlient: CachedOauth2Client,
    val dpBehandlingBaseUrl: String,
    val dpBehandlingScope: String,
    val httpKlient: HttpClient = configureHttpClient(),
    val opplysningRepository: QuizOpplysningRepository,
) {
    fun hentBarn(søknadId: UUID): List<BarnResponseDTO> {
        val registerBarn =
            opplysningRepository
                .hent(
                    beskrivendeId = beskrivendeIdPdlBarn,
                    søknadId = søknadId,
                )?.svar
                ?.asListOf<BarnSvar>() ?: emptyList()

        val egneBarn =
            opplysningRepository
                .hent(
                    beskrivendeId = beskrivendeIdEgneBarn,
                    søknadId = søknadId,
                )?.svar
                ?.asListOf<BarnSvar>() ?: emptyList()

        return (registerBarn + egneBarn)
            .map {
                val fraRegister = if (it.fraRegister) Kilde.register else Kilde.soknad
                BarnResponseDTO(
                    barnId = it.barnSvarId,
                    opplysninger =
                        listOf(
                            BarnOpplysningDTO("fornavnOgMellomnavn", it.fornavnOgMellomnavn, DataType.tekst, fraRegister),
                            BarnOpplysningDTO("etternavn", it.etternavn, DataType.tekst, fraRegister),
                            BarnOpplysningDTO("fodselsdato", it.fødselsdato.toString(), DataType.dato, fraRegister),
                            BarnOpplysningDTO("oppholdssted", it.statsborgerskap, DataType.land, fraRegister),
                            BarnOpplysningDTO("forsorgerBarnet", it.forsørgerBarnet.toString(), DataType.boolsk, Kilde.soknad),
                            BarnOpplysningDTO("kvalifisererTilBarnetillegg", it.kvalifisererTilBarnetillegg.toString(), DataType.boolsk),
                            BarnOpplysningDTO("barnetilleggFom", it.barnetilleggFom.toString(), DataType.dato),
                            BarnOpplysningDTO("barnetilleggTom", it.barnetilleggTom.toString(), DataType.dato),
                            BarnOpplysningDTO("begrunnelse", it.begrunnelse ?: "", DataType.tekst),
                            BarnOpplysningDTO("endretAv", it.endretAv ?: "", DataType.tekst),
                        ),
                )
            }.toMutableList()
    }

    fun erEndret(
        oppdatertBarn: OppdatertBarnDTO,
        søknadId: UUID,
    ): Boolean {
        val opprinneligOpplysning =
            hentBarn(søknadId).find { it.barnId == oppdatertBarn.barnId }
                ?: throw IllegalArgumentException("Fant ikke barn med id ${oppdatertBarn.barnId}")
        return opprinneligOpplysning.opplysninger.find { it.id == "fornavnOgMellomnavn" }?.verdi != oppdatertBarn.fornavnOgMellomnavn ||
            opprinneligOpplysning.opplysninger.find { it.id == "etternavn" }?.verdi != oppdatertBarn.etternavn ||
            opprinneligOpplysning.opplysninger.find { it.id == "fodselsdato" }?.verdi != oppdatertBarn.fodselsdato.toString() ||
            opprinneligOpplysning.opplysninger.find { it.id == "oppholdssted" }?.verdi != oppdatertBarn.oppholdssted ||
            opprinneligOpplysning.opplysninger.find { it.id == "forsorgerBarnet" }?.verdi != oppdatertBarn.forsorgerBarnet.toString() ||
            opprinneligOpplysning.opplysninger
                .find {
                    it.id == "kvalifisererTilBarnetillegg"
                }?.verdi != oppdatertBarn.kvalifisererTilBarnetillegg.toString() ||
            opprinneligOpplysning.opplysninger.find { it.id == "barnetilleggFom" }?.verdi != oppdatertBarn.barnetilleggFom.toString() ||
            opprinneligOpplysning.opplysninger.find { it.id == "barnetilleggTom" }?.verdi != oppdatertBarn.barnetilleggTom.toString()
    }

    fun oppdaterBarn(
        oppdatertBarnRequest: OppdatertBarnRequestDTO,
        søknadId: UUID,
        saksbehandlerId: String,
        token: String,
    ) {
        val oppdatertBarn = oppdatertBarnRequest.oppdatertBarn

        val opprinneligBarnOpplysninger =
            opplysningRepository.hentAlle(søknadId).filter { it.type == Barn }

        val ident = opprinneligBarnOpplysninger.first().ident

        val alleBarnSvar = opprinneligBarnOpplysninger.flatMap { it.svar.asListOf<BarnSvar>() }

        val opprinneligBarnSvar =
            alleBarnSvar.find { it.barnSvarId == oppdatertBarn.barnId }
                ?: throw IllegalArgumentException("Fant ikke barn med id ${oppdatertBarn.barnId}")

        val oppdatertBarnSvar =
            BarnSvar(
                barnSvarId = oppdatertBarn.barnId,
                fornavnOgMellomnavn = oppdatertBarn.fornavnOgMellomnavn,
                etternavn = oppdatertBarn.etternavn,
                fødselsdato = oppdatertBarn.fodselsdato,
                statsborgerskap = oppdatertBarn.oppholdssted,
                forsørgerBarnet = oppdatertBarn.forsorgerBarnet,
                fraRegister = opprinneligBarnSvar.fraRegister,
                kvalifisererTilBarnetillegg = oppdatertBarn.kvalifisererTilBarnetillegg,
                barnetilleggFom = oppdatertBarn.barnetilleggFom,
                barnetilleggTom = oppdatertBarn.barnetilleggTom,
                begrunnelse = oppdatertBarn.begrunnelse,
                endretAv = saksbehandlerId,
            )

        try {
            sendbarnTilDpBehandling(
                oppdatertBarnRequest = oppdatertBarnRequest,
                søknadId = søknadId,
                ident = ident,
                token = token,
            )
        } catch (e: Exception) {
            logger.error { e.message }
            throw IllegalStateException("Feil ved oppdatering av barn mot dp-behandling", e)
        }

        opplysningRepository.oppdaterBarn(søknadId, oppdatertBarnSvar)
    }

    fun sendbarnTilDpBehandling(
        oppdatertBarnRequest: OppdatertBarnRequestDTO,
        søknadId: UUID,
        ident: String,
        token: String,
    ) {
        val løsningsbarn =
            finnBarn(
                ident = ident,
                søknadId = søknadId,
            )
        val oboToken = azureAdKlient.onBehalfOf(token, dpBehandlingScope)
        val behandlingId = oppdatertBarnRequest.behandlingId
        val opplysningId = oppdatertBarnRequest.opplysningId
        val dpBehandlingBarn =
            DpBehandlingOpplysning(
                verdi = objectMapper.writeValueAsString(løsningsbarn),
                begrunnelse = oppdatertBarnRequest.oppdatertBarn.begrunnelse,
            )

        runBlocking {
            val response: HttpResponse =
                httpKlient.put("$dpBehandlingBaseUrl/$behandlingId/opplysning/$opplysningId") {
                    accept(ContentType.Application.Json)
                    header("Authorization", "Bearer $oboToken")
                    contentType(ContentType.Application.Json)
                    setBody(dpBehandlingBarn)
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

    private fun finnBarn(
        ident: String,
        søknadId: UUID,
    ): List<Løsningsbarn> {
        val pdlBarnSvar = hentBarnSvar(beskrivendeIdPdlBarn, ident, søknadId)
        val egneBarnSvar = hentBarnSvar(beskrivendeIdEgneBarn, ident, søknadId)

        return (pdlBarnSvar + egneBarnSvar).map {
            Løsningsbarn(
                fornavnOgMellomnavn = it.fornavnOgMellomnavn,
                etternavn = it.etternavn,
                fødselsdato = it.fødselsdato,
                statsborgerskap = it.statsborgerskap,
                kvalifiserer = it.kvalifisererTilBarnetillegg,
                barnetilleggFom = it.barnetilleggFom,
                barnetilleggTom = it.barnetilleggTom,
                endretAv = it.endretAv,
                begrunnelse = it.begrunnelse,
            )
        }
    }

    private fun hentBarnSvar(
        beskrivendeId: String,
        ident: String,
        søknadId: UUID,
    ) = opplysningRepository.hent(beskrivendeId, ident, søknadId)?.svar?.asListOf<BarnSvar>() ?: emptyList()

    private companion object {
        private val logger = KotlinLogging.logger {}
    }
}

data class DpBehandlingOpplysning(
    val verdi: String,
    val begrunnelse: String,
)
