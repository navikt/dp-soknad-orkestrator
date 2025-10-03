package no.nav.dagpenger.soknad.orkestrator.søknad.seksjon

import no.nav.dagpenger.soknad.orkestrator.utils.Html
import no.nav.dagpenger.soknad.orkestrator.utils.genererPdfFraHtml
import no.nav.dagpenger.soknad.orkestrator.utils.søknadCss
import java.util.UUID

class SeksjonService(
    val seksjonRepository: SeksjonRepository,
) {
    fun lagre(
        søknadId: UUID,
        seksjonId: String,
        json: String,
    ) {
        seksjonRepository.lagre(søknadId, seksjonId, json)
    }

    fun hent(
        søknadId: UUID,
        seksjonId: String,
    ): String? = seksjonRepository.hent(søknadId, seksjonId)

    fun hentAlle(søknadId: UUID): List<Seksjon> = seksjonRepository.hentSeksjoner(søknadId)

    fun hentLagredeSeksjonerForGittSøknadId(søknadId: UUID): List<String> = seksjonRepository.hentFullførteSeksjoner(søknadId)

    fun journalførSøknadHtml(søknadHtmlString: String): ByteArray {
        val html = Html(søknadHtmlString)
        val htmlWithCss = html.leggTilCss(søknadCss)
        val pdf = genererPdfFraHtml(htmlWithCss)

        // Lagre i bucket og få urn
        // Send til journalføring (vi må vel ha med søknadId/journalpostId e.l?)
        // Send løsning på behov

        return pdf
    }
}

data class Seksjon(
    val seksjonId: String,
    val data: String,
)
