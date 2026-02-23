package no.nav.dagpenger.soknad.orkestrator.søknad.pdf

import freemarker.template.Configuration
import freemarker.template.Configuration.VERSION_2_3_34
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

        init {
            freemarkerConfiguration.setClassForTemplateLoading(this::class.java, "/pdf-maler")
            freemarkerConfiguration.defaultEncoding = "UTF-8"
        }
    }

    private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

    fun genererBruttoPdfPayload(
        søknadId: UUID,
        ident: String,
    ): String = genererPdfPayload(søknadId, ident, "brutto-pdf.ftl")

    fun genererNettoPdfPayload(
        søknadId: UUID,
        ident: String,
    ): String = genererPdfPayload(søknadId, ident, "netto-pdf.ftl")

    private fun genererPdfPayload(
        søknadId: UUID,
        ident: String,
        mal: String,
    ): String {
        val søknad = søknadRepository.hent(søknadId) ?: throw IllegalStateException("Fant ikke søknad $søknadId")
        val søknadPersonalia =
            søknadPersonaliaRepository.hent(søknadId, ident)
                ?: throw IllegalStateException("Fant ikke personalia for søknad $søknadId")
        val pdfGrunnlag =
            seksjonRepository
                .hentPdfGrunnlag(søknadId, ident)
                .also { if (it.isEmpty()) throw IllegalStateException("Fant ikke PDF-grunnlag for søknad $søknadId") }
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
}
