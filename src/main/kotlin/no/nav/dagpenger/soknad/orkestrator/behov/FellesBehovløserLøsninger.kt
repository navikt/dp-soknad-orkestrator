package no.nav.dagpenger.soknad.orkestrator.behov

import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonRepository
import no.nav.dagpenger.soknad.orkestrator.utils.erBoolean
import java.time.LocalDate
import java.util.UUID

open class FellesBehovløserLøsninger(
    val opplysningRepository: QuizOpplysningRepository,
    val søknadRepository: SøknadRepository,
    val seksjonRepository: SeksjonRepository,
) {
    fun harSøkerenHattArbeidsforholdIEøs(
        beskrivendeId: String,
        ident: String,
        søknadId: UUID,
    ): Boolean {
        val svarPåBehov =
            opplysningRepository.hent(beskrivendeId, ident, søknadId)?.svar

        if (svarPåBehov != null) {
            return svarPåBehov.toString().toBoolean()
        }

        val seksjonsSvar =
            try {
                seksjonRepository.hentSeksjonsvarEllerKastException(
                    ident,
                    søknadId,
                    "arbeidsforhold",
                )
            } catch (e: IllegalStateException) {
                return false
            }

        objectMapper.readTree(seksjonsSvar).let { seksjonsJson ->
            seksjonsJson.findPath("harDuJobbetIEtAnnetEøsLandSveitsEllerStorbritanniaILøpetAvDeSiste36Månedene")?.let {
                if (!it.isMissingOrNull()) {
                    return it.erBoolean()
                }
            }
        }

        return false
    }

    fun ønskerDagpengerFraDato(
        ident: String,
        søknadId: UUID,
    ): LocalDate {
        val seksjonsSvar =
            seksjonRepository.hentSeksjonsvarEllerKastException(
                ident,
                søknadId,
                "din-situasjon",
            )

        objectMapper.readTree(seksjonsSvar).let { seksjonssJson ->
            val dagpengerFraDato = seksjonssJson.findPath("hvilkenDatoSøkerDuDagpengerFra")
            val gjenopptakFraDato = seksjonssJson.findPath("hvilkenDatoSøkerDuGjenopptakFra")

            if (!dagpengerFraDato.isMissingOrNull()) {
                return dagpengerFraDato.asLocalDate()
            } else if (!gjenopptakFraDato.isMissingOrNull()) {
                return gjenopptakFraDato.asLocalDate()
            }
        }

        throw IllegalStateException(
            "Fant ingen opplysning på behov ønskerDagpengerFraDato for søknad med id: $søknadId",
        )
    }
}
