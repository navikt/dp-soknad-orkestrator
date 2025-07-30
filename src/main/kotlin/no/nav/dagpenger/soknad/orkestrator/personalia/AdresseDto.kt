package no.nav.dagpenger.soknad.orkestrator.personalia

data class AdresseDto(
    val adresselinje1: String = "",
    val adresselinje2: String = "",
    val adresselinje3: String = "",
    val postnummer: String = "",
    val poststed: String? = "",
    val landkode: String = "",
    val land: String = "",
)
