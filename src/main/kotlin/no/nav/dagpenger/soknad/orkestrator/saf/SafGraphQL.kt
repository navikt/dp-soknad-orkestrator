package no.nav.dagpenger.soknad.orkestrator.saf

import no.nav.dagpenger.soknad.orkestrator.config.objectMapper

internal data class SafJournalpost(
    val dokumenter: List<SafDokumentInfo>,
) {
    val hovedDokument: SafDokumentInfo get() = dokumenter.first()

    companion object {
        fun fraGraphQlJson(json: String): SafJournalpost {
            val journalpostNode = objectMapper.readTree(json).path("data").path("journalpost")
            require(!journalpostNode.isMissingNode && !journalpostNode.isNull) {
                "SAF-respons mangler data.journalpost"
            }
            return objectMapper.treeToValue(journalpostNode, SafJournalpost::class.java)
        }
    }
}

internal data class SafDokumentInfo(
    val dokumentInfoId: String,
    val brevkode: String?,
    val tittel: String?,
)
