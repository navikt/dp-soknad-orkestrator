package no.nav.dagpenger.soknad.orkestrator.behov.løsere

import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.dagpenger.soknad.orkestrator.behov.Behovløser
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov.BarnOver16
import no.nav.dagpenger.soknad.orkestrator.behov.Behovmelding
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.asListOf
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.BarnSvar
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonRepository
import java.time.LocalDate
import java.util.UUID

class BarnOver16Behovløser(
    rapidsConnection: RapidsConnection,
    opplysningRepository: QuizOpplysningRepository,
    søknadRepository: SøknadRepository,
    seksjonRepository: SeksjonRepository,
) : Behovløser(rapidsConnection, opplysningRepository, søknadRepository, seksjonRepository) {
    override val behov = BarnOver16.name
    override val beskrivendeId
        get() = throw NotImplementedError("Overrider løs() og trenger ikke beskrivendeId")

    companion object {
        const val BESKRIVENDE_ID_PDL_BARN = "faktum.register.barn-liste"
        const val BESKRIVENDE_ID_EGNE_BARN = "faktum.barn-liste"
    }

    override fun løs(behovmelding: Behovmelding) {
        val svarPåBehov = harBarnOver16(behovmelding.ident, behovmelding.søknadId)
        publiserLøsning(behovmelding, svarPåBehov)
    }

    internal fun harBarnOver16(
        ident: String,
        søknadId: UUID,
    ): Boolean {
        val sekstensårsDatoGrense = LocalDate.now().minusYears(16)

        // 1. Quiz-opplysninger (gammel søknad)
        val pdlBarnSvar = hentBarnSvar(BESKRIVENDE_ID_PDL_BARN, ident, søknadId)
        val egneBarnSvar = hentBarnSvar(BESKRIVENDE_ID_EGNE_BARN, ident, søknadId)

        if ((pdlBarnSvar + egneBarnSvar).isNotEmpty()) {
            return (pdlBarnSvar + egneBarnSvar).any { barnSvar -> barnSvar.fødselsdato <= sekstensårsDatoGrense }
        }

        // 2. Seksjon (ny søknad)
        val seksjonsvar =
            seksjonRepository.hentSeksjonsvar(søknadId, ident, "barnetillegg")
                ?: return false

        val alleBarn =
            objectMapper.readTree(seksjonsvar).let { seksjonJson ->
                (seksjonJson.findPath("barnFraPdl")?.toList() ?: emptyList()) +
                    (seksjonJson.findPath("barnLagtManuelt")?.toList() ?: emptyList())
            }

        return alleBarn.any { barnJson ->
            barnJson["fødselsdato"]?.asLocalDate()?.let { fødselsdato -> fødselsdato <= sekstensårsDatoGrense } ?: false
        }
    }

    private fun hentBarnSvar(
        beskrivendeId: String,
        ident: String,
        søknadId: UUID,
    ) = opplysningRepository.hent(beskrivendeId, ident, søknadId)?.svar?.asListOf<BarnSvar>() ?: emptyList()
}
