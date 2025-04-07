package no.nav.dagpenger.soknad.orkestrator.opplysning

import no.nav.dagpenger.soknad.orkestrator.api.models.BarnOpplysningDTO
import no.nav.dagpenger.soknad.orkestrator.api.models.BarnOpplysningDTO.DataType
import no.nav.dagpenger.soknad.orkestrator.api.models.BarnOpplysningDTO.Kilde
import no.nav.dagpenger.soknad.orkestrator.api.models.BarnResponseDTO
import no.nav.dagpenger.soknad.orkestrator.api.models.OppdatertBarnDTO
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.BarnetilleggBehovLøser.Companion.beskrivendeIdEgneBarn
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.BarnetilleggBehovLøser.Companion.beskrivendeIdPdlBarn
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.asListOf
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Barn
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.BarnSvar
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepository
import java.util.UUID

class OpplysningService(val opplysningRepository: QuizOpplysningRepository) {
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

        return (registerBarn + egneBarn).map {
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
            opprinneligOpplysning.opplysninger.find {
                it.id == "kvalifisererTilBarnetillegg"
            }?.verdi != oppdatertBarn.kvalifisererTilBarnetillegg.toString() ||
            opprinneligOpplysning.opplysninger.find { it.id == "barnetilleggFom" }?.verdi != oppdatertBarn.barnetilleggFom.toString() ||
            opprinneligOpplysning.opplysninger.find { it.id == "barnetilleggTom" }?.verdi != oppdatertBarn.barnetilleggTom.toString()
    }

    fun oppdaterBarn(
        oppdatertBarn: OppdatertBarnDTO,
        søknadId: UUID,
        saksbehandlerId: String,
    ) {
        val opprinneligBarnOpplysninger =
            opplysningRepository.hentAlle(søknadId).filter { it.type == Barn }

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

        opplysningRepository.oppdaterBarn(søknadId, oppdatertBarnSvar)
    }
}
