package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.dagpenger.soknad.orkestrator.behov.Behovløser
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.Barnetillegg
import no.nav.dagpenger.soknad.orkestrator.behov.Behovmelding
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.asListOf
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.BarnSvar
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepository
import java.time.LocalDate
import java.util.UUID

class BarnetilleggBehovLøser(
    rapidsConnection: RapidsConnection,
    opplysningRepository: QuizOpplysningRepository,
) : Behovløser(rapidsConnection, opplysningRepository) {
    override val behov = Barnetillegg.name
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

    internal data class Løsningsbarn(
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
