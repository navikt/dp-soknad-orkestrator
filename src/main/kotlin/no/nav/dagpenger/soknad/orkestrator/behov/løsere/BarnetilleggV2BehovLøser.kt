package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.dagpenger.soknad.orkestrator.behov.Behovløser
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.BarnetilleggV2
import no.nav.dagpenger.soknad.orkestrator.behov.Behovmelding
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.asListOf
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.BarnSvar
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepository
import java.time.LocalDate
import java.util.UUID

class BarnetilleggV2BehovLøser(
    rapidsConnection: RapidsConnection,
    opplysningRepository: QuizOpplysningRepository,
) : Behovløser(rapidsConnection, opplysningRepository) {
    override val behov = BarnetilleggV2.name
    override val beskrivendeId
        get() = throw NotImplementedError("Overrider løs() og trenger ikke beskrivendeId")

    override fun løs(behovmelding: Behovmelding) {
        val svarPåBehov = finnBarn(behovmelding.ident, behovmelding.søknadId)
        publiserLøsning(behovmelding, svarPåBehov)
    }

    companion object {
        val beskrivendeIdPdlBarn = "faktum.register.barn-liste"
        val beskrivendeIdEgneBarn = "faktum.barn-liste"
    }

    private fun finnBarn(
        ident: String,
        søknadId: UUID,
    ): BarnetilleggV2Løsning {
        val pdlBarnSvar = hentBarnSvar(beskrivendeIdPdlBarn, ident, søknadId)
        val egneBarnSvar = hentBarnSvar(beskrivendeIdEgneBarn, ident, søknadId)

        val søknadbarnId =
            opplysningRepository.mapTilSøknadbarnId(søknadId)
                ?: throw RuntimeException("Fant ikke søknadbarnId for søknad $søknadId, selv om barn finnes i søknaden")

        val alleBarn =
            (pdlBarnSvar + egneBarnSvar).map {
                Løsningsbarn(
                    søknadbarnId = søknadbarnId,
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

        return BarnetilleggV2Løsning(
            søknadbarnId = søknadbarnId,
            barn = alleBarn,
        )
    }

    private fun hentBarnSvar(
        beskrivendeId: String,
        ident: String,
        søknadId: UUID,
    ) = opplysningRepository.hent(beskrivendeId, ident, søknadId)?.svar?.asListOf<BarnSvar>() ?: emptyList()

    internal data class BarnetilleggV2Løsning(
        val søknadbarnId: UUID,
        val barn: List<Løsningsbarn>,
    )

    internal data class Løsningsbarn(
        val søknadbarnId: UUID,
        val fornavnOgMellomnavn: String,
        val etternavn: String,
        val fødselsdato: LocalDate,
        val statsborgerskap: String,
        val kvalifiserer: Boolean,
        val barnetilleggFom: LocalDate?,
        val barnetilleggTom: LocalDate?,
        val endretAv: String?,
        val begrunnelse: String?,
    )
}
