package no.nav.dagpenger.soknad.orkestrator.utils

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import java.io.ByteArrayOutputStream

fun genererPdfFraHtml(html: String): ByteArray {
    val vasketHtml = vaskHtml(html)

    val outputStream = ByteArrayOutputStream()
    PdfRendererBuilder()
        .withHtmlContent(vasketHtml, null)
        .toStream(outputStream)
        .run()

    return outputStream.toByteArray()
}

fun vaskHtml(html: String): String =
    html
        .let { lukkSelfClosingTags(it) }
        .let { fjernNonBreakingSpace(it) }

private fun lukkSelfClosingTags(html: String): String {
    val selfClosingTags = listOf("br", "img", "input", "hr", "meta", "link")
    val selfClosingTagMønster = Regex("<(${selfClosingTags.joinToString("|")})([^>]*)>")

    return html.replace(selfClosingTagMønster) { matchResult ->
        "<${matchResult.groupValues[1]}${matchResult.groupValues[2]} />"
    }
}

private fun fjernNonBreakingSpace(html: String): String = html.replace(Regex("&nbsp;"), " ")
