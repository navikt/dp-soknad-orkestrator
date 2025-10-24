package no.nav.dagpenger.soknad.orkestrator.søknad.pdf

import freemarker.template.Configuration
import freemarker.template.Configuration.VERSION_2_3_34
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import kotlin.text.Charsets.UTF_8

class PdfService {
    companion object {
        var freemarkerConfiguration: Configuration = Configuration(VERSION_2_3_34)

        init {
            freemarkerConfiguration.setClassForTemplateLoading(this::class.java, "/pdf-maler")
            freemarkerConfiguration.defaultEncoding = "UTF-8"
        }
    }

    fun genererBruttoPdf(json: String): String = genererPdf("brutto-pdf.ftl", json)

    fun genererNettoPdf(json: String): String = genererPdf("netto-pdf.ftl", json)

    internal fun genererPdf(
        mal: String,
        json: String,
    ): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        freemarkerConfiguration
            .getTemplate(mal)
            .process(hashMapOf(Pair("json", json)), OutputStreamWriter(byteArrayOutputStream))
        return byteArrayOutputStream.toByteArray().toString(UTF_8)
    }
}
