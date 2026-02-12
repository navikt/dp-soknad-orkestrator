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

            logger.info { "Løste behov med quiz-data" }
            sikkerlogg.info { "Løste behov med quiz-data" }

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
            seksjonRepository.hentSeksjonsvar(
                søknadId,
                ident,
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
                val kvalifisererTilBarnetillegg = barnJson["forsørgerDuBarnet"]?.asText() == "ja"
                val fødselsdato = barnJson["fødselsdato"].asLocalDate()
                val barnetilleggperiode = if (kvalifisererTilBarnetillegg) barnetilleggperiode(fødselsdato) else null

                Løsningsbarn(
                    søknadbarnId = randomUUID(),
                    fornavnOgMellomnavn = barnJson["fornavnOgMellomnavn"].asText(),
                    etternavn = barnJson["etternavn"].asText(),
                    fødselsdato = fødselsdato,
                    statsborgerskap = barnJson["bostedsland"].asText(),
                    kvalifiserer = kvalifisererTilBarnetillegg,
                    barnetilleggFom = barnetilleggperiode?.first,
                    barnetilleggTom = barnetilleggperiode?.second,
                    endretAv = null,
                    begrunnelse = null,
                )
            }

        logger.info { "Løste behov med orkestrator-data" }
        sikkerlogg.info { "Løste behov med orkestrator-data" }

        return svar
    }

    internal fun barnetilleggperiode(fødselsdato: LocalDate): Pair<LocalDate, LocalDate> = fødselsdato to fødselsdato.plusYears(18)

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
