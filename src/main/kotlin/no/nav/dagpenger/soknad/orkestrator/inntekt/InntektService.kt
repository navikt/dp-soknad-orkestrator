package no.nav.dagpenger.soknad.orkestrator.inntekt

import mu.KotlinLogging
import java.util.UUID

class InntektService {
    fun hentMinsteinntektGrunnlag(søknadId: UUID): MinsteinntektGrunnlag {
        logger.info { "Henter forelagt inntekt for søknadId: $søknadId" }

        return MinsteinntektGrunnlag(
            siste12mnd = "100000",
            siste36mnd = "200000",
        )
    }

    fun lagreSvar(
        søknadId: UUID,
        svar: ForeleggingMinsteinntektGrunnlagSvarDTO,
    ) {
        logger.info { "Mottatt svar: $svar for søknad: $søknadId" }
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
        private val sikkerlogg = KotlinLogging.logger("tjenestekall.InntektService")
    }
}

data class MinsteinntektGrunnlag(
    val siste12mnd: String,
    val siste36mnd: String,
)

data class ForeleggingMinsteinntektGrunnlagSvarDTO(
    val søknadId: String,
    val bekreftet: Boolean,
    val begrunnelse: String?,
)
