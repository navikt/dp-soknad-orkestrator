package no.nav.dagpenger.soknad.orkestrator.søknad

import com.fasterxml.jackson.databind.JsonNode
import de.slub.urn.URN
import java.util.UUID

data class NyJournalpost(
    val hovedDokument: Dokument,
    val dokumenter: List<Dokument>,
)

data class NyEttersendingJournalPost(
    val dokumenter: List<Dokument>,
    val dokumentasjonskravJson: String,
    val seksjonId: String,
)

data class Dokument(
    val skjemakode: String,
    val varianter: List<Dokumentvariant>,
)

data class Dokumentvariant(
    val uuid: UUID = UUID.randomUUID(),
    val filnavn: String,
    val urn: String,
    val variant: String,
    val type: String,
) {
    init {
        kotlin
            .runCatching {
                URN.rfc8141().parse(urn)
            }.onFailure {
                throw IllegalArgumentException("Ikke gyldig URN: $urn")
            }
    }
}

fun JsonNode.dokumentVarianter(): List<Dokumentvariant> =
    this.toList().map { node ->
        val format =
            when (node["metainfo"]["variant"].asText()) {
                "NETTO" -> {
                    "ARKIV"
                }

                "BRUTTO" -> {
                    "FULLVERSJON"
                }

                else -> {
                    throw kotlin.IllegalArgumentException(
                        "Ukjent joarkvariant, se https://confluence.adeo.no/display/BOA/Variantformat",
                    )
                }
            }
        Dokumentvariant(
            filnavn = node["metainfo"]["innhold"].asText(),
            urn = node["urn"].asText(),
            variant = format,
            type = node["metainfo"]["filtype"].asText(),
        )
    }
