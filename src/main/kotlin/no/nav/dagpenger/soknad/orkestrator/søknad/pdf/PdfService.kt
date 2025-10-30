package no.nav.dagpenger.soknad.orkestrator.søknad.pdf

import freemarker.template.Configuration
import freemarker.template.Configuration.VERSION_2_3_34
import no.nav.dagpenger.soknad.orkestrator.personalia.AdresseDto
import no.nav.dagpenger.soknad.orkestrator.personalia.PersonDto
import no.nav.dagpenger.soknad.orkestrator.personalia.PersonaliaDto
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonRepository
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.time.LocalDate
import java.util.UUID
import kotlin.text.Charsets.UTF_8

class PdfService(
    val søknadRepository: SøknadRepository,
    val seksjonRepository: SeksjonRepository,
) {
    companion object {
        private var freemarkerConfiguration: Configuration = Configuration(VERSION_2_3_34)

        init {
            freemarkerConfiguration.setClassForTemplateLoading(this::class.java, "/pdf-maler")
            freemarkerConfiguration.defaultEncoding = "UTF-8"
        }
    }

    fun genererBruttoPdfPayload(
        ident: String,
        søknadId: UUID,
    ): String = genererPdfPayload(ident, søknadId, "brutto-pdf.ftl")

    fun genererNettoPdfPayload(
        ident: String,
        søknadId: UUID,
    ): String = genererPdfPayload(ident, søknadId, "netto-pdf.ftl")

    private fun genererPdfPayload(
        ident: String,
        søknadId: UUID,
        mal: String,
    ): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        freemarkerConfiguration
            .getTemplate(mal)
            .process(
                hashMapOf(Pair("json", genererPdfGrunnlagMedSøknadMetadata(søknadId, ident))),
                OutputStreamWriter(byteArrayOutputStream),
            )
        return byteArrayOutputStream.toByteArray().toString(UTF_8)
    }

    private fun genererPdfGrunnlagMedSøknadMetadata(
        søknadId: UUID,
        ident: String,
    ): String {
        val søknad = søknadRepository.hent(søknadId) ?: throw IllegalStateException("Fant ikke søknad $søknadId")

        // TODO: Bytt ut med kall til en eller annen tjeneste, mulig at PersonaliaService kan brukes med en annen token elns.
        // TODO: Feilhåndtering hvis personalia-oppsalg ikke går så bra.
        val personaliaDto =
            PersonaliaDto(
                PersonDto(
                    ident = "987654321",
                    fornavn = "Dag",
                    mellomnavn = "Petter",
                    etternavn = "Engesøker",
                    fodselsDato = LocalDate.now(),
                    postAdresse = null,
                    folkeregistrertAdresse =
                        AdresseDto(
                            "Adresselinje1",
                            "Adresselinje2",
                            "Adresselinje3",
                            "1234",
                            "Poststedet",
                            "",
                            "Land",
                        ),
                ),
                "01583408300",
            )

        val hentPdfGrunnlag =
            seksjonRepository
                .hentPdfGrunnlag(ident, søknadId)
                .also { if (it.isEmpty()) throw IllegalStateException("Fant ikke PDF-grunnlag for søknad $søknadId") }
                .joinToString(",")

        val pdfGrunnlagMedSøknadMetadata =
            //language=json
            """
            {
              "ident": "$ident",
              "navn": "${personaliaDto.person.fornavn} ${personaliaDto.person.mellomnavn} ${personaliaDto.person.etternavn}",
              "adresse": "${lagAdresse(personaliaDto.person.folkeregistrertAdresse)}",
              "innsendtTidspunkt": "${søknad.innsendtTidspunkt}",
              "kontonummer": "${personaliaDto.kontonummer}",
              "seksjoner": [$hentPdfGrunnlag]
            }
            """.trimIndent()

        return pdfGrunnlagMedSøknadMetadata
    }

    private fun lagAdresse(folkeregistrertAdresse: AdresseDto?): String =
        if (folkeregistrertAdresse != null) {
            listOf(
                folkeregistrertAdresse.adresselinje1,
                folkeregistrertAdresse.adresselinje2,
                folkeregistrertAdresse.adresselinje3,
                "${folkeregistrertAdresse.postnummer} ${folkeregistrertAdresse.poststed}",
                folkeregistrertAdresse.land,
            ).filter { verdi -> verdi.isNotBlank() }.joinToString(", ")
        } else {
            ""
        }
}
