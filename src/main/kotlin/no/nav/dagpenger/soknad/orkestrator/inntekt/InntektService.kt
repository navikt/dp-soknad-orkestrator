package no.nav.dagpenger.soknad.orkestrator.inntekt

import mu.KotlinLogging
import no.nav.dagpenger.soknad.orkestrator.api.models.ForeleggingresultatDTO
import no.nav.dagpenger.soknad.orkestrator.api.models.MinsteinntektGrunnlagDTO
import java.util.UUID

class InntektService {
    fun hentMinsteinntektGrunnlag(søknadId: UUID): MinsteinntektGrunnlagDTO {
        logger.info { "Henter forelagt inntekt for søknadId: $søknadId" }

        return MinsteinntektGrunnlagDTO(
            siste12mnd = "100000",
            siste36mnd = "200000",
        )
    }

    fun lagreSvar(
        søknadId: UUID,
        svar: ForeleggingresultatDTO,
    ) {
        logger.info { "Mottatt svar: $svar for søknad: $søknadId" }
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
        private val sikkerlogg = KotlinLogging.logger("tjenestekall.InntektService")
    }
}
