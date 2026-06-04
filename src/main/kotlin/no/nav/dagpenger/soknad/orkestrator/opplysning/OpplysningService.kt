package no.nav.dagpenger.soknad.orkestrator.opplysning

import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.soknad.orkestrator.api.models.BarnDataDTO
import no.nav.dagpenger.soknad.orkestrator.api.models.BarnOpplysningDTO
import no.nav.dagpenger.soknad.orkestrator.api.models.BarnOpplysningDTO.DataType
import no.nav.dagpenger.soknad.orkestrator.api.models.BarnOpplysningDTO.Kilde
import no.nav.dagpenger.soknad.orkestrator.api.models.BarnRequestDTO
import no.nav.dagpenger.soknad.orkestrator.api.models.BarnResponseDTO
import no.nav.dagpenger.soknad.orkestrator.api.models.SlettBarnRequestDTO
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
import tools.jackson.databind.JsonNode
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
        val kilde =
            when {
                fraRegister -> Kilde.register
                endretAv != null -> Kilde.saksbehandler
                else -> Kilde.soknad
            }
        // forsørgerBarnet kommer alltid fra søker eller saksbehandler, aldri fra register
        val forsørgerKilde = if (endretAv != null) Kilde.saksbehandler else Kilde.soknad
        return BarnResponseDTO(
            barnId = barnSvarId,
            opplysninger =
                listOf(
                    BarnOpplysningDTO(
                        BarnOpplysningDTO.Id.fornavnOgMellomnavn,
                        fornavnOgMellomnavn,
                        DataType.tekst,
                        kilde,
                    ),
                    BarnOpplysningDTO(BarnOpplysningDTO.Id.etternavn, etternavn, DataType.tekst, kilde),
                    BarnOpplysningDTO(BarnOpplysningDTO.Id.fodselsdato, fødselsdato.toString(), DataType.dato, kilde),
                    BarnOpplysningDTO(BarnOpplysningDTO.Id.oppholdssted, statsborgerskap, DataType.land, kilde),
                    BarnOpplysningDTO(
                        BarnOpplysningDTO.Id.forsorgerBarnet,
                        forsørgerBarnet.toString(),
                        DataType.boolsk,
                        forsørgerKilde,
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
        barn: BarnDataDTO,
        barnId: UUID,
        søknadId: UUID,
    ): Boolean {
        val opprinneligOpplysning =
            hentBarn(søknadId).find { it.barnId == barnId }
                ?: throw IllegalArgumentException("Fant ikke barn med id $barnId")
        return opprinneligOpplysning.opplysninger.find { it.id == BarnOpplysningDTO.Id.fornavnOgMellomnavn }?.verdi !=
            barn.fornavnOgMellomnavn ||
            opprinneligOpplysning.opplysninger.find { it.id == BarnOpplysningDTO.Id.fodselsdato }?.verdi !=
            barn.fodselsdato.toString() ||
            opprinneligOpplysning.opplysninger.find { it.id == BarnOpplysningDTO.Id.oppholdssted }?.verdi != barn.oppholdssted ||
            opprinneligOpplysning.opplysninger.find { it.id == BarnOpplysningDTO.Id.forsorgerBarnet }?.verdi !=
            barn.forsorgerBarnet.toString() ||
            opprinneligOpplysning.opplysninger
                .find {
                    it.id == BarnOpplysningDTO.Id.kvalifisererTilBarnetillegg
                }?.verdi != barn.kvalifisererTilBarnetillegg.toString() ||
            opprinneligOpplysning.opplysninger.find { it.id == BarnOpplysningDTO.Id.barnetilleggFom }?.verdi !=
            (barn.barnetilleggFom?.toString() ?: "") ||
            opprinneligOpplysning.opplysninger.find { it.id == BarnOpplysningDTO.Id.barnetilleggTom }?.verdi !=
            (barn.barnetilleggTom?.toString() ?: "") ||
            opprinneligOpplysning.opplysninger.find { it.id == BarnOpplysningDTO.Id.etternavn }?.verdi != barn.etternavn
    }

    fun oppdaterBarn(
        barnRequest: BarnRequestDTO,
        barnId: UUID,
        søknadId: UUID,
        saksbehandlerId: String,
        token: String,
    ) {
        val barn = barnRequest.barn

        val alleBarnSvar = hentAlleBarnSvar(søknadId)

        val opprinneligBarnSvar =
            alleBarnSvar.find { it.barnSvarId == barnId }
                ?: throw IllegalArgumentException("Fant ikke barn med id $barnId")

        val oppdatertBarnSvar =
            BarnSvar(
                barnSvarId = barnId,
                fornavnOgMellomnavn = barn.fornavnOgMellomnavn,
                etternavn = barn.etternavn,
                fødselsdato = barn.fodselsdato,
                statsborgerskap = barn.oppholdssted,
                forsørgerBarnet = barn.forsorgerBarnet,
                fraRegister = opprinneligBarnSvar.fraRegister,
                kvalifisererTilBarnetillegg = barn.kvalifisererTilBarnetillegg,
                barnetilleggFom = barn.barnetilleggFom,
                barnetilleggTom = barn.barnetilleggTom,
                begrunnelse = barn.begrunnelse,
                endretAv = saksbehandlerId,
            )

        val alleBarnEtterEndring = alleBarnSvar.map { if (it.barnSvarId == barnId) oppdatertBarnSvar else it }

        val søknadbarnId = opplysningRepository.hentEllerOpprettSøknadbarnId(søknadId)

        try {
            sendbarnTilDpBehandling(
                barnRequest = barnRequest,
                alleBarn = alleBarnEtterEndring,
                token = token,
                søknadbarnId = søknadbarnId,
            )
        } catch (e: Exception) {
            logger.error { e.message }
            throw IllegalStateException("Feil ved oppdatering av barn mot dp-behandling", e)
        }

        saksbehandlerBarnRepository.lagreBarn(søknadId, alleBarnEtterEndring, saksbehandlerId)
    }

    fun slettBarn(
        slettRequest: SlettBarnRequestDTO,
        barnId: UUID,
        søknadId: UUID,
        saksbehandlerId: String,
        token: String,
    ) {
        val alleBarnSvar = hentAlleBarnSvar(søknadId)

        require(alleBarnSvar.any { it.barnSvarId == barnId }) {
            "Fant ikke barn med id $barnId"
        }

        val alleBarnEtterSletting = alleBarnSvar.filter { it.barnSvarId != barnId }

        val søknadbarnId = opplysningRepository.hentEllerOpprettSøknadbarnId(søknadId)
        val løsningsbarn =
            alleBarnEtterSletting.map {
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
                begrunnelse = slettRequest.begrunnelse,
                gyldigFraOgMed = tidligsteBarnetilleggFom(alleBarnEtterSletting),
            )

        try {
            dpBehandlingKlient.oppdaterBarnOpplysning(
                behandlingId = slettRequest.behandlingId,
                dpBehandlingOpplysning = dpBehandlingOpplysning,
                token = token,
            )
        } catch (e: Exception) {
            logger.error { e.message }
            throw IllegalStateException("Feil ved sletting av barn mot dp-behandling", e)
        }

        saksbehandlerBarnRepository.lagreBarn(søknadId, alleBarnEtterSletting, saksbehandlerId)
    }

    fun leggTilBarn(
        barnRequest: BarnRequestDTO,
        søknadId: UUID,
        saksbehandlerId: String,
        token: String,
    ): List<BarnResponseDTO> {
        require(søknadRepository.hent(søknadId) != null) { "Fant ikke søknad med id $søknadId" }

        val barn = barnRequest.barn
        val nyttBarnSvar =
            BarnSvar(
                barnSvarId = UUID.randomUUID(),
                fornavnOgMellomnavn = barn.fornavnOgMellomnavn,
                etternavn = barn.etternavn,
                fødselsdato = barn.fodselsdato,
                statsborgerskap = barn.oppholdssted,
                forsørgerBarnet = barn.forsorgerBarnet,
                fraRegister = false,
                kvalifisererTilBarnetillegg = barn.kvalifisererTilBarnetillegg,
                barnetilleggFom = if (barn.kvalifisererTilBarnetillegg) barnetilleggperiode(barn.fodselsdato).first else null,
                barnetilleggTom = if (barn.kvalifisererTilBarnetillegg) barnetilleggperiode(barn.fodselsdato).second else null,
                endretAv = saksbehandlerId,
                begrunnelse = barn.begrunnelse,
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
                begrunnelse = barn.begrunnelse,
                gyldigFraOgMed = tidligsteBarnetilleggFom(alleBarnEtterEndring),
            )

        val behandlingId = barnRequest.behandlingId
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
        barnRequest: BarnRequestDTO,
        alleBarn: List<BarnSvar>,
        token: String,
        søknadbarnId: UUID,
    ) {
        val barn = barnRequest.barn
        val løsningsbarn =
            alleBarn.map {
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
                begrunnelse = barn.begrunnelse,
                gyldigFraOgMed = tidligsteBarnetilleggFom(alleBarn),
            )

        val behandlingId =
            barnRequest.behandlingId
                ?: throw IllegalArgumentException("behandlingId er påkrevd for oppdatering av barn")

        dpBehandlingKlient.oppdaterBarnOpplysning(
            behandlingId = behandlingId,
            dpBehandlingOpplysning = dpBehandlingOpplysning,
            token = token,
        )
    }

    fun mapTilSøknadId(søknadbarnId: UUID): UUID =
        opplysningRepository.mapTilSøknadId(søknadbarnId)
            ?: throw IllegalArgumentException("Fant ikke søknadId for søknadbarnId $søknadbarnId")

    private companion object {
        private val logger = KotlinLogging.logger {}

        fun tidligsteBarnetilleggFom(barn: List<BarnSvar>) =
            barn.filter { it.kvalifisererTilBarnetillegg }.mapNotNull { it.barnetilleggFom }.minOrNull()
    }
}
