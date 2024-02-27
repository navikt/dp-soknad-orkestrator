package no.nav.dagpenger.soknad.orkestrator.søknadmottak

data class Fakta(
    val beskrivendeId: String,
    val id: String,
    val svar: List<String>,
    val type: String,
)
