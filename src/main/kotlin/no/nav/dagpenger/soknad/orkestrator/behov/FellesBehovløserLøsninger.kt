package no.nav.dagpenger.soknad.orkestrator.behov

import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import no.nav.dagpenger.soknad.orkestrator.behov.Behovløser.Companion.logger
import no.nav.dagpenger.soknad.orkestrator.behov.Behovløser.Companion.sikkerlogg
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
            logger.info { "Løste behov med quiz-data" }
            sikkerlogg.info { "Løste behov med quiz-data" }

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
                    logger.info { "Løste behov med orkestrator-data" }
                    sikkerlogg.info { "Løste behov med orkestrator-data" }

                    return it.erBoolean()
                }
            }
        }

        return false
    }

    fun ønskerDagpengerFraDato(
        ident: String,
        søknadId: UUID,
        behov: String,
    ): LocalDate {
        val beskrivendeIdSøknadsdato = "faktum.dagpenger-soknadsdato"
        val beskrivendeIdGjenopptaksdato = "faktum.arbeidsforhold.gjenopptak.soknadsdato-gjenopptak"
        val dagpengerFraDatoFelt = "hvilkenDatoSøkerDuDagpengerFra"
        val gjenopptakFraDatoFelt = "hvilkenDatoSøkerDuGjenopptakFra"

        val svarPåBehov =
            opplysningRepository.hent(beskrivendeIdSøknadsdato, ident, søknadId)?.svar
                ?: opplysningRepository.hent(beskrivendeIdGjenopptaksdato, ident, søknadId)?.svar

        if (svarPåBehov != null) {
            logger.info { "Løste behov med quiz-data" }
            sikkerlogg.info { "Løste behov med quiz-data" }

            return svarPåBehov.toString().let { LocalDate.parse(it) }
        }

        val seksjonsSvar =
            seksjonRepository.hentSeksjonsvarEllerKastException(
                ident,
                søknadId,
                "din-situasjon",
            )

        objectMapper.readTree(seksjonsSvar).let { seksjonssJson ->
            val dagpengerFraDato = seksjonssJson.findPath(dagpengerFraDatoFelt)
            val gjenopptakFraDato = seksjonssJson.findPath(gjenopptakFraDatoFelt)

            if (!dagpengerFraDato.isMissingOrNull()) {
                logger.info { "Løste behov med orkestrator-data" }
                sikkerlogg.info { "Løste behov med orkestrator-data" }

                return dagpengerFraDato.asLocalDate()
            } else if (!gjenopptakFraDato.isMissingOrNull()) {
                logger.info { "Løste behov med orkestrator-data" }
                sikkerlogg.info { "Løste behov med orkestrator-data" }

                return gjenopptakFraDato.asLocalDate()
            }
        }

        throw IllegalStateException(
            "Fant ingen opplysning på behov $behov for søknad med id: $søknadId",
        )
    }

    fun harSøkerenAvtjentVerneplikt(
        behov: String,
        beskrivendeId: String,
        ident: String,
        søknadId: UUID,
    ): Boolean {
        val svarPåBehov =
            opplysningRepository.hent(beskrivendeId, ident, søknadId)?.svar

        if (svarPåBehov != null) {
            logger.info { "Løste behov med quiz-data" }
            sikkerlogg.info { "Løste behov med quiz-data" }

            return svarPåBehov.toString().toBoolean()
        }
        val seksjonsSvar =
            seksjonRepository.hentSeksjonsvarEllerKastException(
                ident,
                søknadId,
                "verneplikt",
            )

        objectMapper.readTree(seksjonsSvar).let { seksjonsJson ->
            seksjonsJson.findPath("avtjentVerneplikt")?.let {
                if (!it.isMissingOrNull()) {
                    logger.info { "Løste behov med orkestrator-data" }
                    sikkerlogg.info { "Løste behov med orkestrator-data" }

                    return it.erBoolean()
                }
            }
        }

        throw IllegalStateException(
            "Fant ingen opplysning på behov $behov for søknad med id: $søknadId",
        )
    }
}
