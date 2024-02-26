package no.nav.søknad

data class Seksjoner(
    val beskrivendeId: String,
    val fakta: List<Fakta>,
    val ferdig: Boolean,
)
