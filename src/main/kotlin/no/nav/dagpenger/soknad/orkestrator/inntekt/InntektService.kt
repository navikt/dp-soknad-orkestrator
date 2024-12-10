package no.nav.dagpenger.soknad.orkestrator.inntekt

import mu.KotlinLogging
import java.util.UUID

class InntektService {
    fun hentForelagtOpplysning(søknadId: UUID): ForelagtInntekt {
        logger.info { "Henter forelagt inntekt for søknadId: $søknadId" }

        return ForelagtInntekt(
            siste1År = "1000000",
            siste3År = "1000000",
        )
    }

    fun lagreSvar(
        søknadId: UUID,
        svar: InntektsvarDTO,
    ) {
        logger.info { "Mottatt svar: $svar for søknad: $søknadId" }
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
        private val sikkerlogg = KotlinLogging.logger("tjenestekall.InntektService")
    }
}

data class ForelagtInntekt(
    val siste1År: String,
    val siste3År: String,
)

data class InntektsvarDTO(
    val søknadId: String,
    val bekreftet: Boolean,
    val begrunnelse: String?,
)
