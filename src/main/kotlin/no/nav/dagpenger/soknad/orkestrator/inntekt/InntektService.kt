package no.nav.dagpenger.soknad.orkestrator.inntekt

import mu.KotlinLogging
import no.nav.dagpenger.soknad.orkestrator.api.models.ForeleggingresultatDTO
import no.nav.dagpenger.soknad.orkestrator.api.models.HtmlDokumentDTO
import no.nav.dagpenger.soknad.orkestrator.api.models.MinsteinntektGrunnlagDTO
import no.nav.dagpenger.soknad.orkestrator.journalføring.JournalføringService
import no.nav.dagpenger.soknad.orkestrator.utils.genererPdfFraHtml
import java.util.UUID

class InntektService(
    val journalføringService: JournalføringService,
) {
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

    fun hentForeleggingresultat(søknadId: UUID): ForeleggingresultatDTO {
        logger.info { "Henter foreleggingresultat for søknadId: $søknadId" }

        return ForeleggingresultatDTO(
            søknadId = søknadId,
            bekreftet = false,
            begrunnelse = "Alt er feil",
        )
    }

    fun journalfør(
        søknadId: UUID,
        html: HtmlDokumentDTO,
        personident: String,
    ) {
        logger.info("Journalfør PDF basert på html for søknadId: $søknadId")

        val pdf = genererPdfFraHtml(html.html)

        journalføringService.sendJournalførMinidialogBehov(
            ident = personident,
            søknadId = søknadId,
            dialogId = UUID.randomUUID(),
            pdf = pdf,
            json = html.html,
        )
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
        private val sikkerlogg = KotlinLogging.logger("tjenestekall.InntektService")
    }
}
