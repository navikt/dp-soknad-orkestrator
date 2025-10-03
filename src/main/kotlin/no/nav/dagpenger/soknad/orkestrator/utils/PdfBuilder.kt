package no.nav.dagpenger.soknad.orkestrator.utils

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import java.io.ByteArrayOutputStream

fun genererPdfFraHtml(html: Html): ByteArray {
    val outputStream = ByteArrayOutputStream()
    PdfRendererBuilder()
        .withHtmlContent(html.verdi, null)
        .toStream(outputStream)
        .run()

    return outputStream.toByteArray()
}
