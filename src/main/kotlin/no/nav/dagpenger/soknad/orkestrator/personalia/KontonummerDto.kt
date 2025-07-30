package no.nav.dagpenger.soknad.orkestrator.personalia

data class KontonummerDto(
    val kontonummer: String? = null,
    val banknavn: String? = null,
    val bankLandkode: String? = null,
) {
    companion object {
        val TOM = KontonummerDto()
    }
}
