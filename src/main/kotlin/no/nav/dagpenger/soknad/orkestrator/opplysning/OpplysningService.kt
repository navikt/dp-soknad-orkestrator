package no.nav.dagpenger.soknad.orkestrator.opplysning

import no.nav.dagpenger.soknad.orkestrator.api.models.BarnOpplysningDTO
import no.nav.dagpenger.soknad.orkestrator.api.models.BarnOpplysningDTO.DataType
import no.nav.dagpenger.soknad.orkestrator.api.models.BarnOpplysningDTO.Kilde
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
            val fraRegister = if (it.fraRegister) Kilde.register else Kilde.søknad
            BarnResponseDTO(
                barnId = it.barnSvarId,
                opplysninger =
                    listOf(
                        BarnOpplysningDTO("fornavnOgMellomnavn", it.fornavnOgMellomnavn, DataType.tekst, fraRegister),
                        BarnOpplysningDTO("etternavn", it.etternavn, DataType.tekst, fraRegister),
                        BarnOpplysningDTO("fødselsdato", it.fødselsdato.toString(), DataType.dato, fraRegister),
                        BarnOpplysningDTO("oppholdssted", it.statsborgerskap, DataType.land, fraRegister),
                        BarnOpplysningDTO("forsørgerBarnet", it.forsørgerBarnet.toString(), DataType.boolsk, Kilde.søknad),
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
        opplysning: OppdatertBarnRequestDTO,
        søknadId: UUID,
    ): Boolean {
        val opprinneligOpplysning =
            hentBarn(søknadId).find { it.barnId == opplysning.barnId }
                ?: throw IllegalArgumentException("Fant ikke barn med id ${opplysning.barnId}")
        return opprinneligOpplysning.opplysninger.find { it.id == "fornavnOgMellomnavn" }?.verdi != opplysning.fornavnOgMellomnavn ||
            opprinneligOpplysning.opplysninger.find { it.id == "etternavn" }?.verdi != opplysning.etternavn ||
            opprinneligOpplysning.opplysninger.find { it.id == "fødselsdato" }?.verdi != opplysning.fødselsdato.toString() ||
            opprinneligOpplysning.opplysninger.find { it.id == "oppholdssted" }?.verdi != opplysning.oppholdssted ||
            opprinneligOpplysning.opplysninger.find { it.id == "forsørgerBarnet" }?.verdi != opplysning.forsørgerBarnet.toString() ||
            opprinneligOpplysning.opplysninger.find {
                it.id == "kvalifisererTilBarnetillegg"
            }?.verdi != opplysning.kvalifisererTilBarnetillegg.toString() ||
            opprinneligOpplysning.opplysninger.find { it.id == "barnetilleggFom" }?.verdi != opplysning.barnetilleggFom.toString() ||
            opprinneligOpplysning.opplysninger.find { it.id == "barnetilleggTom" }?.verdi != opplysning.barnetilleggTom.toString()
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
