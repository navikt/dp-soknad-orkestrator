package no.nav.dagpenger.soknad.orkestrator.søknad.pdf

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import freemarker.template.Configuration
import freemarker.template.Configuration.VERSION_2_3_34
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import no.nav.dagpenger.soknad.orkestrator.søknad.SøknadPersonalia
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadPersonaliaRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonRepository
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.text.Charsets.UTF_8

class PdfPayloadService(
    val søknadRepository: SøknadRepository,
    val søknadPersonaliaRepository: SøknadPersonaliaRepository,
    val seksjonRepository: SeksjonRepository,
) {
    companion object {
        private var freemarkerConfiguration: Configuration = Configuration(VERSION_2_3_34)
        private val felterMedMarkup = setOf("label", "description")
        private val ikkeEscapetAmpersand = Regex("&(?!#\\d+;|#x[0-9a-fA-F]+;|[a-zA-Z][a-zA-Z0-9]+;)")

        init {
            freemarkerConfiguration.setClassForTemplateLoading(this::class.java, "/pdf-maler")
            freemarkerConfiguration.defaultEncoding = "UTF-8"
            freemarkerConfiguration.outputFormat = freemarker.core.HTMLOutputFormat.INSTANCE
        }
    }

    private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

    fun genererBruttoPdfPayload(
        søknadId: UUID,
        ident: String,
    ): String = genererPdfPayload(søknadId, ident, "brutto-pdf.ftlh", normaliserMarkupFelter = true)

    fun genererNettoPdfPayload(
        søknadId: UUID,
        ident: String,
    ): String = genererPdfPayload(søknadId, ident, "netto-pdf.ftlh", normaliserMarkupFelter = false)

    private fun genererPdfPayload(
        søknadId: UUID,
        ident: String,
        mal: String,
        normaliserMarkupFelter: Boolean,
    ): String {
        val søknad = søknadRepository.hent(søknadId) ?: throw IllegalStateException("Fant ikke søknad $søknadId")
        val søknadPersonalia =
            søknadPersonaliaRepository.hent(søknadId, ident)
                ?: throw IllegalStateException("Fant ikke personalia for søknad $søknadId")
        val pdfGrunnlag =
            seksjonRepository
                .hentPdfGrunnlag(søknadId, ident)
                .also { if (it.isEmpty()) throw IllegalStateException("Fant ikke PDF-grunnlag for søknad $søknadId") }
                .map { if (normaliserMarkupFelter) normaliserHtmlFelterISeksjon(it) else it }
                .joinToString(",")

        val dokumentasjonskrav =
            seksjonRepository
                .hentDokumentasjonskrav(søknadId, ident)
                .joinToString(",")

        val pdfGrunnlagMedSøknadMetadata =
            //language=json
            """
            {
              "personalia": {
                  "ident": "$ident",
                  "navn": "${søknadPersonalia.fornavn} ${søknadPersonalia.mellomnavn} ${søknadPersonalia.etternavn}",
                  "adresse": "${lagAdresse(søknadPersonalia)}",
                  "kontonummer": "${søknadPersonalia.kontonummer}"
              },
              "innsendtTidspunkt": "${søknad.innsendtTidspunkt?.format(dateTimeFormatter)}",
              "seksjoner": [$pdfGrunnlag],
              "dokumentasjonskrav": [$dokumentasjonskrav]
            }
            """.trimIndent()

        val byteArrayOutputStream = ByteArrayOutputStream()
        freemarkerConfiguration
            .getTemplate(mal)
            .process(
                hashMapOf(Pair("json", pdfGrunnlagMedSøknadMetadata)),
                OutputStreamWriter(byteArrayOutputStream),
            )
        return byteArrayOutputStream.toByteArray().toString(UTF_8)
    }

    private fun lagAdresse(søknadPersonalia: SøknadPersonalia): String =
        listOfNotNull(
            søknadPersonalia.adresselinje1,
            søknadPersonalia.adresselinje2,
            søknadPersonalia.adresselinje3,
            "${søknadPersonalia.postnummer} ${søknadPersonalia.poststed}",
            søknadPersonalia.land,
        ).filter { verdi -> verdi.isNotBlank() }.joinToString(", ")

    private fun normaliserHtmlFelterISeksjon(seksjonSomJson: String): String {
        val seksjonNode = objectMapper.readTree(seksjonSomJson)
        normaliserHtmlFelter(seksjonNode)
        return objectMapper.writeValueAsString(seksjonNode)
    }

    private fun normaliserHtmlFelter(node: JsonNode) {
        when (node) {
            is ObjectNode -> {
                val felter = node.properties().iterator()
                while (felter.hasNext()) {
                    val (feltNavn, feltVerdi) = felter.next()
                    if (feltNavn in felterMedMarkup && feltVerdi.isTextual) {
                        node.put(feltNavn, normaliserRichText(feltVerdi.asText()))
                    } else {
                        normaliserHtmlFelter(feltVerdi)
                    }
                }
            }

            is ArrayNode -> {
                node.forEach { normaliserHtmlFelter(it) }
            }
        }
    }

    private fun normaliserRichText(tekst: String): String = tekst.replace(ikkeEscapetAmpersand, "&amp;")
}
