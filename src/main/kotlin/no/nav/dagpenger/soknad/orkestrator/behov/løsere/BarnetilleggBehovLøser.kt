package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.dagpenger.soknad.orkestrator.behov.Behovløser
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.Barnetillegg
import no.nav.dagpenger.soknad.orkestrator.behov.Behovmelding
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.asListOf
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.BarnSvar
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonRepository
import java.time.LocalDate
import java.util.UUID
import java.util.UUID.randomUUID

class BarnetilleggBehovLøser(
    rapidsConnection: RapidsConnection,
    opplysningRepository: QuizOpplysningRepository,
    søknadRepository: SøknadRepository,
    seksjonRepository: SeksjonRepository,
) : Behovløser(rapidsConnection, opplysningRepository, søknadRepository, seksjonRepository) {
    override val behov = Barnetillegg.name
    override val beskrivendeId
        get() = throw NotImplementedError("Overrider løs() og trenger ikke beskrivendeId")

    override fun løs(behovmelding: Behovmelding) {
        val svarPåBehov = finnBarn(behovmelding.ident, behovmelding.søknadId)
        publiserLøsning(behovmelding, svarPåBehov)
    }

    companion object {
        const val BESKRIVENDE_ID_PDL_BARN = "faktum.register.barn-liste"
        const val BESKRIVENDE_ID_EGNE_BARN = "faktum.barn-liste"
    }

    private fun finnBarn(
        ident: String,
        søknadId: UUID,
    ): List<Løsningsbarn> {
        val pdlBarnSvar = hentBarnSvar(BESKRIVENDE_ID_PDL_BARN, ident, søknadId)
        val egneBarnSvar = hentBarnSvar(BESKRIVENDE_ID_EGNE_BARN, ident, søknadId)

        if ((pdlBarnSvar + egneBarnSvar).isNotEmpty()) {
            val søknadbarnId = opplysningRepository.hentEllerOpprettSøknadbarnId(søknadId)

            return (pdlBarnSvar + egneBarnSvar).map {
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
        }

        val seksjonsvar =
            seksjonRepository?.hentSeksjonsvar(
                ident,
                søknadId,
                "barnetillegg",
            ) ?: return emptyList()

        val pdlBarn =
            objectMapper.readTree(seksjonsvar).let { seksjonJson ->
                seksjonJson.findPath("barnFraPdl")?.toList() ?: emptyList()
            }

        val egneBarn =
            objectMapper.readTree(seksjonsvar).let { seksjonJson ->
                seksjonJson.findPath("barnLagtManuelt")?.toList() ?: emptyList()
            }

        val svar =
            (pdlBarn + egneBarn).map { barnJson ->
                // TODO: Vi har vel ikke alt vi trenger her?
                Løsningsbarn(
                    søknadbarnId = randomUUID(),
                    fornavnOgMellomnavn = barnJson["fornavn-og-mellomnavn"].asText(),
                    etternavn = barnJson["etternavn"].asText(),
                    fødselsdato = barnJson["fødselsdato"].asLocalDate(),
                    statsborgerskap = barnJson["bostedsland"].asText(),
                    kvalifiserer = false,
                    barnetilleggFom = null,
                    barnetilleggTom = null,
                    endretAv = null,
                    begrunnelse = null,
                )
            }

        return svar
    }

    private fun hentBarnSvar(
        beskrivendeId: String,
        ident: String,
        søknadId: UUID,
    ) = opplysningRepository.hent(beskrivendeId, ident, søknadId)?.svar?.asListOf<BarnSvar>() ?: emptyList()

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
