package no.nav.dagpenger.soknad.orkestrator.opplysning

import no.nav.dagpenger.soknad.orkestrator.api.models.BarnResponseDTO
import no.nav.dagpenger.soknad.orkestrator.api.models.OppdatertBarnRequestDTO
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.BarnetilleggBehovLøser.Companion.beskrivendeIdEgneBarn
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.BarnetilleggBehovLøser.Companion.beskrivendeIdPdlBarn
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.asListOf
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.BarnSvar
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepository
import java.util.UUID

class OpplysningService(val opplysningRepository: QuizOpplysningRepository) {
    fun hentBarn(søknadId: UUID): List<no.nav.dagpenger.soknad.orkestrator.api.models.BarnResponseDTO> {
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

        return (registerBarn + egneBarn).map {
            BarnResponseDTO(
                barnId = it.barnId,
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
}
