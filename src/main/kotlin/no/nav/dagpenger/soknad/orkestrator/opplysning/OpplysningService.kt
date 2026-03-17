package no.nav.dagpenger.soknad.orkestrator.opplysning

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.soknad.orkestrator.api.models.BarnOpplysningDTO
import no.nav.dagpenger.soknad.orkestrator.api.models.BarnOpplysningDTO.DataType
import no.nav.dagpenger.soknad.orkestrator.api.models.BarnOpplysningDTO.Kilde
import no.nav.dagpenger.soknad.orkestrator.api.models.BarnResponseDTO
import no.nav.dagpenger.soknad.orkestrator.api.models.NyttBarnRequestDTO
import no.nav.dagpenger.soknad.orkestrator.api.models.OppdatertBarnDTO
import no.nav.dagpenger.soknad.orkestrator.api.models.OppdatertBarnRequestDTO
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.BarnetilleggBehovLøser.Companion.BESKRIVENDE_ID_EGNE_BARN
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.BarnetilleggBehovLøser.Companion.BESKRIVENDE_ID_PDL_BARN
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.BarnetilleggV2BehovLøser.BarnetilleggV2Løsning
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.BarnetilleggV2BehovLøser.LøsningsbarnV2
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.asListOf
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Barn.barnetilleggperiode
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.BarnSvar
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonRepository
import java.util.UUID

class OpplysningService(
    val opplysningRepository: QuizOpplysningRepository,
    val dpBehandlingKlient: DpBehandlingKlient,
    val søknadRepository: SøknadRepository,
    val saksbehandlerBarnRepository: SaksbehandlerBarnRepository,
    val seksjonRepository: SeksjonRepository,
) {
    fun hentBarn(søknadId: UUID): List<BarnResponseDTO> = hentAlleBarnSvar(søknadId).map { it.tilBarnResponseDTO() }

    internal fun hentAlleBarnSvar(søknadId: UUID): List<BarnSvar> {
        // 1. Saksbehandler-redigerte barn (nyeste snapshot)
        saksbehandlerBarnRepository.hentBarn(søknadId)?.let { return it }

        // 2. Quiz-opplysninger (gammel søknad)
        val quizBarn = hentBarnFraQuizOpplysninger(søknadId)
        if (quizBarn.isNotEmpty()) return quizBarn

        // 3. Seksjon v2 (ny søknad)
        return hentBarnFraSeksjon(søknadId)
    }

    private fun hentBarnFraQuizOpplysninger(søknadId: UUID): List<BarnSvar> {
        val registerBarn =
            opplysningRepository
                .hent(beskrivendeId = BESKRIVENDE_ID_PDL_BARN, søknadId = søknadId)
                ?.svar
                ?.asListOf<BarnSvar>() ?: emptyList()

        val egneBarn =
            opplysningRepository
                .hent(beskrivendeId = BESKRIVENDE_ID_EGNE_BARN, søknadId = søknadId)
                ?.svar
                ?.asListOf<BarnSvar>() ?: emptyList()

        return registerBarn + egneBarn
    }

    private fun hentBarnFraSeksjon(søknadId: UUID): List<BarnSvar> {
        val ident =
            søknadRepository.hent(søknadId)?.ident ?: return emptyList()

        val seksjonsvar =
            seksjonRepository.hentSeksjonsvar(søknadId, ident, "barnetillegg") ?: return emptyList()

        val seksjonJson = objectMapper.readTree(seksjonsvar)
        val pdlBarn = seksjonJson.findPath("barnFraPdl")?.toList() ?: emptyList()
        val egneBarn = seksjonJson.findPath("barnLagtManuelt")?.toList() ?: emptyList()

        fun JsonNode.tilBarnSvar(fraRegister: Boolean): BarnSvar {
            val kvalifiserer = this["forsørgerDuBarnet"]?.asText() == "ja"
            val fødselsdato = this["fødselsdato"].asLocalDate()
            val barnetilleggperiode = if (kvalifiserer) barnetilleggperiode(fødselsdato) else null

            return BarnSvar(
                barnSvarId = this["id"]?.asText()?.let { UUID.fromString(it) } ?: UUID.randomUUID(),
                fornavnOgMellomnavn = this["fornavnOgMellomnavn"].asText(),
                etternavn = this["etternavn"].asText(),
                fødselsdato = fødselsdato,
                statsborgerskap = this["bostedsland"].asText(),
                forsørgerBarnet = kvalifiserer,
                fraRegister = fraRegister,
                kvalifisererTilBarnetillegg = kvalifiserer,
                barnetilleggFom = barnetilleggperiode?.first,
                barnetilleggTom = barnetilleggperiode?.second,
                endretAv = null,
                begrunnelse = null,
            )
        }

        return pdlBarn.map { it.tilBarnSvar(fraRegister = true) } +
            egneBarn.map { it.tilBarnSvar(fraRegister = false) }
    }

    private fun BarnSvar.tilBarnResponseDTO(): BarnResponseDTO {
        val fraRegister = if (this.fraRegister) Kilde.register else Kilde.soknad
        return BarnResponseDTO(
            barnId = barnSvarId,
            opplysninger =
                listOf(
                    BarnOpplysningDTO(
                        BarnOpplysningDTO.Id.fornavnOgMellomnavn,
                        fornavnOgMellomnavn,
                        DataType.tekst,
                        fraRegister,
                    ),
                    BarnOpplysningDTO(BarnOpplysningDTO.Id.etternavn, etternavn, DataType.tekst, fraRegister),
                    BarnOpplysningDTO(BarnOpplysningDTO.Id.fodselsdato, fødselsdato.toString(), DataType.dato, fraRegister),
                    BarnOpplysningDTO(BarnOpplysningDTO.Id.oppholdssted, statsborgerskap, DataType.land, fraRegister),
                    BarnOpplysningDTO(
                        BarnOpplysningDTO.Id.forsorgerBarnet,
                        forsørgerBarnet.toString(),
                        DataType.boolsk,
                        Kilde.soknad,
                    ),
                    BarnOpplysningDTO(
                        BarnOpplysningDTO.Id.kvalifisererTilBarnetillegg,
                        kvalifisererTilBarnetillegg.toString(),
                        DataType.boolsk,
                    ),
                    BarnOpplysningDTO(BarnOpplysningDTO.Id.barnetilleggFom, barnetilleggFom?.toString() ?: "", DataType.dato),
                    BarnOpplysningDTO(BarnOpplysningDTO.Id.barnetilleggTom, barnetilleggTom?.toString() ?: "", DataType.dato),
                    BarnOpplysningDTO(BarnOpplysningDTO.Id.begrunnelse, begrunnelse ?: "", DataType.tekst),
                ),
        )
    }

    fun erEndret(
        oppdatertBarn: OppdatertBarnDTO,
        søknadId: UUID,
    ): Boolean {
        val opprinneligOpplysning =
            hentBarn(søknadId).find { it.barnId == oppdatertBarn.barnId }
                ?: throw IllegalArgumentException("Fant ikke barn med id ${oppdatertBarn.barnId}")
        return opprinneligOpplysning.opplysninger.find { it.id == BarnOpplysningDTO.Id.fornavnOgMellomnavn }?.verdi !=
            oppdatertBarn.fornavnOgMellomnavn ||
            opprinneligOpplysning.opplysninger.find { it.id == BarnOpplysningDTO.Id.fodselsdato }?.verdi !=
            oppdatertBarn.fodselsdato.toString() ||
            opprinneligOpplysning.opplysninger.find { it.id == BarnOpplysningDTO.Id.oppholdssted }?.verdi != oppdatertBarn.oppholdssted ||
            opprinneligOpplysning.opplysninger.find { it.id == BarnOpplysningDTO.Id.forsorgerBarnet }?.verdi !=
            oppdatertBarn.forsorgerBarnet.toString() ||
            opprinneligOpplysning.opplysninger
                .find {
                    it.id == BarnOpplysningDTO.Id.kvalifisererTilBarnetillegg
                }?.verdi != oppdatertBarn.kvalifisererTilBarnetillegg.toString() ||
            opprinneligOpplysning.opplysninger.find { it.id == BarnOpplysningDTO.Id.barnetilleggFom }?.verdi !=
            (oppdatertBarn.barnetilleggFom?.toString() ?: "") ||
            opprinneligOpplysning.opplysninger.find { it.id == BarnOpplysningDTO.Id.barnetilleggTom }?.verdi !=
            (oppdatertBarn.barnetilleggTom?.toString() ?: "")
    }

    fun oppdaterBarn(
        oppdatertBarnRequest: OppdatertBarnRequestDTO,
        søknadId: UUID,
        saksbehandlerId: String,
        token: String,
    ) {
        val oppdatertBarn = oppdatertBarnRequest.oppdatertBarn

        val alleBarnSvar = hentAlleBarnSvar(søknadId)

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

        val uendredeBarn = alleBarnSvar.filter { it.barnSvarId != oppdatertBarn.barnId }
        val alleBarnEtterEndring = uendredeBarn + oppdatertBarnSvar

        val søknadbarnId = opplysningRepository.hentEllerOpprettSøknadbarnId(søknadId)

        try {
            sendbarnTilDpBehandling(
                oppdatertBarnRequest = oppdatertBarnRequest,
                token = token,
                søknadbarnId = søknadbarnId,
                uendredeBarn = uendredeBarn,
                oppdatertBarnEndretAv = saksbehandlerId,
            )
        } catch (e: Exception) {
            logger.error { e.message }
            throw IllegalStateException("Feil ved oppdatering av barn mot dp-behandling", e)
        }

        saksbehandlerBarnRepository.lagreBarn(søknadId, alleBarnEtterEndring, saksbehandlerId)
    }

    fun leggTilBarn(
        nyttBarnRequest: NyttBarnRequestDTO,
        søknadId: UUID,
        saksbehandlerId: String,
        token: String,
    ): List<BarnResponseDTO> {
        require(søknadRepository.hent(søknadId) != null) { "Fant ikke søknad med id $søknadId" }

        val nyttBarn = nyttBarnRequest.nyttBarn
        val nyttBarnSvar =
            BarnSvar(
                barnSvarId = UUID.randomUUID(),
                fornavnOgMellomnavn = nyttBarn.fornavnOgMellomnavn,
                etternavn = nyttBarn.etternavn,
                fødselsdato = nyttBarn.fodselsdato,
                statsborgerskap = nyttBarn.oppholdssted,
                forsørgerBarnet = nyttBarn.forsorgerBarnet,
                fraRegister = false,
                kvalifisererTilBarnetillegg = nyttBarn.kvalifisererTilBarnetillegg,
                barnetilleggFom = if (nyttBarn.kvalifisererTilBarnetillegg) barnetilleggperiode(nyttBarn.fodselsdato).first else null,
                barnetilleggTom = if (nyttBarn.kvalifisererTilBarnetillegg) barnetilleggperiode(nyttBarn.fodselsdato).second else null,
                endretAv = saksbehandlerId,
                begrunnelse = nyttBarn.begrunnelse,
            )

        val eksisterendeBarn = hentAlleBarnSvar(søknadId)
        val alleBarnEtterEndring = eksisterendeBarn + nyttBarnSvar

        val søknadbarnId = opplysningRepository.hentEllerOpprettSøknadbarnId(søknadId)

        val løsningsbarn =
            alleBarnEtterEndring.map {
                LøsningsbarnV2(
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

        val dpBehandlingOpplysning =
            NyOpplysningDTO(
                verdi = objectMapper.writeValueAsString(BarnetilleggV2Løsning(søknadbarnId, løsningsbarn)),
                begrunnelse = nyttBarn.begrunnelse,
                gyldigFraOgMed = nyttBarnSvar.barnetilleggFom,
                gyldigTilOgMed = nyttBarnSvar.barnetilleggTom,
            )

        val behandlingId = nyttBarnRequest.behandlingId
        if (behandlingId == null) {
            logger.warn { "behandlingId er null, sender ikke barn til dp-behandling" }
        } else {
            try {
                dpBehandlingKlient.oppdaterBarnOpplysning(
                    behandlingId = behandlingId,
                    dpBehandlingOpplysning = dpBehandlingOpplysning,
                    token = token,
                )
            } catch (e: Exception) {
                logger.error { e.message }
                throw IllegalStateException("Feil ved sending av barn til dp-behandling", e)
            }
        }

        saksbehandlerBarnRepository.lagreBarn(søknadId, alleBarnEtterEndring, saksbehandlerId)

        return hentBarn(søknadId)
    }

    fun sendbarnTilDpBehandling(
        oppdatertBarnRequest: OppdatertBarnRequestDTO,
        token: String,
        uendredeBarn: List<BarnSvar>,
        oppdatertBarnEndretAv: String,
        søknadbarnId: UUID,
    ) {
        val oppdatertBarn = oppdatertBarnRequest.oppdatertBarn
        val løsningsbarn =
            uendredeBarn
                .map {
                    LøsningsbarnV2(
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
                }.toMutableList()
                .plus(
                    LøsningsbarnV2(
                        fornavnOgMellomnavn = oppdatertBarn.fornavnOgMellomnavn,
                        etternavn = oppdatertBarn.etternavn,
                        fødselsdato = oppdatertBarn.fodselsdato,
                        statsborgerskap = oppdatertBarn.oppholdssted,
                        kvalifiserer = oppdatertBarn.kvalifisererTilBarnetillegg,
                        barnetilleggFom = oppdatertBarn.barnetilleggFom,
                        barnetilleggTom = oppdatertBarn.barnetilleggTom,
                        endretAv = oppdatertBarnEndretAv,
                        begrunnelse = oppdatertBarn.begrunnelse,
                    ),
                )

        val dpBehandlingOpplysning =
            NyOpplysningDTO(
                verdi = objectMapper.writeValueAsString(BarnetilleggV2Løsning(søknadbarnId, løsningsbarn)),
                begrunnelse = oppdatertBarnRequest.oppdatertBarn.begrunnelse,
                gyldigFraOgMed = oppdatertBarnRequest.oppdatertBarn.barnetilleggFom,
                gyldigTilOgMed = oppdatertBarnRequest.oppdatertBarn.barnetilleggTom,
            )

        dpBehandlingKlient.oppdaterBarnOpplysning(
            behandlingId = oppdatertBarnRequest.behandlingId,
            dpBehandlingOpplysning = dpBehandlingOpplysning,
            token = token,
        )
    }

    fun mapTilSøknadId(søknadbarnId: UUID): UUID =
        opplysningRepository.mapTilSøknadId(søknadbarnId)
            ?: throw IllegalArgumentException("Fant ikke søknadId for søknadbarnId $søknadbarnId")

    private companion object {
        private val logger = KotlinLogging.logger {}
    }
}
