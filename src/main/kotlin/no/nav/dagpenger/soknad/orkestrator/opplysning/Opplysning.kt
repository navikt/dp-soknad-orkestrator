package no.nav.dagpenger.soknad.orkestrator.opplysning

class Opplysning(private val svar: List<String>, private val beskrivendeId: String) {
    fun svar() = svar

    fun beskrivendeId() = beskrivendeId
}
