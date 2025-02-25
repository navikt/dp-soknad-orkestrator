package no.nav.dagpenger.soknad.orkestrator.opplysning

import mu.KotlinLogging
import no.nav.dagpenger.soknad.orkestrator.api.models.BarnResponseDTO
import no.nav.dagpenger.soknad.orkestrator.api.models.OppdatertBarnRequestDTO
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.BarnetilleggBehovLøser.Companion.beskrivendeIdEgneBarn
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.BarnetilleggBehovLøser.Companion.beskrivendeIdPdlBarn
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.asListOf
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Barn
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.BarnSvar
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepository
import java.util.UUID

class OpplysningService(val opplysningRepository: QuizOpplysningRepository) {
    private companion object {
        private val logger = KotlinLogging.logger {}
        private val sikkerlogg = KotlinLogging.logger("tjenestekall.OpplysningService")
    }

    fun hentBarn(søknadId: UUID): List<BarnResponseDTO> {
        val registerBarn =
            opplysningRepository.hent(
                beskrivendeId = beskrivendeIdPdlBarn,
                søknadId = søknadId,
            )?.svar?.asListOf<BarnSvar>() ?: emptyList()

        val egneBarn =
            opplysningRepository.hent(
                beskrivendeId = beskrivendeIdEgneBarn,
                søknadId = søknadId,
            )?.svar?.asListOf<BarnSvar>() ?: emptyList()

        sikkerlogg.info { "Antall registerbarn hentet: ${registerBarn.size}, antall egne barn hentet: ${egneBarn.size}" }

        return (registerBarn + egneBarn).map {
            BarnResponseDTO(
                barnId = it.barnSvarId,
                fornavnOgMellomnavn = it.fornavnOgMellomnavn,
                etternavn = it.etternavn,
                fødselsdato = it.fødselsdato,
                oppholdssted = it.statsborgerskap,
                forsørgerBarnet = it.forsørgerBarnet,
                fraRegister = it.fraRegister,
                kvalifisererTilBarnetillegg = it.kvalifisererTilBarnetillegg,
                barnetilleggFom = it.barnetilleggFom,
                barnetilleggTom = it.barnetilleggTom,
                begrunnelse = it.begrunnelse,
                endretAv = it.endretAv,
            )
        }
    }

    fun erEndret(
        opplysning: OppdatertBarnRequestDTO,
        søknadId: UUID,
    ): Boolean {
        val opprinneligOpplysning =
            hentBarn(søknadId).find { it.barnId == opplysning.barnId }
                ?: throw IllegalArgumentException("Fant ikke barn med id ${opplysning.barnId}")

        return opprinneligOpplysning.fornavnOgMellomnavn != opplysning.fornavnOgMellomnavn ||
            opprinneligOpplysning.etternavn != opplysning.etternavn ||
            opprinneligOpplysning.fødselsdato != opplysning.fødselsdato ||
            opprinneligOpplysning.oppholdssted != opplysning.oppholdssted ||
            opprinneligOpplysning.forsørgerBarnet != opplysning.forsørgerBarnet ||
            opprinneligOpplysning.kvalifisererTilBarnetillegg != opplysning.kvalifisererTilBarnetillegg ||
            opprinneligOpplysning.barnetilleggFom != opplysning.barnetilleggFom ||
            opprinneligOpplysning.barnetilleggTom != opplysning.barnetilleggTom
    }

    fun oppdaterBarn(
        oppdatering: OppdatertBarnRequestDTO,
        søknadId: UUID,
        saksbehandlerId: String,
    ) {
        val opprinneligBarnOpplysning =
            opplysningRepository.hentAlle(søknadId).find { it.type == Barn }
                ?: throw IllegalArgumentException("Fant ikke opplysning om barn for søknad med id $søknadId")

        val opprinneligBarnSvar =
            opprinneligBarnOpplysning.svar.asListOf<BarnSvar>().find { it.barnSvarId == oppdatering.barnId }
                ?: throw IllegalArgumentException("Fant ikke barn med id ${oppdatering.barnId}")

        val oppdatertBarnSvar =
            BarnSvar(
                barnSvarId = oppdatering.barnId,
                fornavnOgMellomnavn = oppdatering.fornavnOgMellomnavn,
                etternavn = oppdatering.etternavn,
                fødselsdato = oppdatering.fødselsdato,
                statsborgerskap = oppdatering.oppholdssted,
                forsørgerBarnet = oppdatering.forsørgerBarnet,
                fraRegister = opprinneligBarnSvar.fraRegister,
                kvalifisererTilBarnetillegg = oppdatering.kvalifisererTilBarnetillegg,
                barnetilleggFom = oppdatering.barnetilleggFom,
                barnetilleggTom = oppdatering.barnetilleggTom,
                begrunnelse = oppdatering.begrunnelse,
                endretAv = saksbehandlerId,
            )

        opplysningRepository.oppdaterBarn(søknadId, oppdatertBarnSvar)
    }
}
