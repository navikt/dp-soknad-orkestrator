package no.nav.dagpenger.soknad.orkestrator.søknadmottak

data class Seksjoner(
    val beskrivendeId: String,
    val fakta: List<Fakta>,
    val ferdig: Boolean,
)
