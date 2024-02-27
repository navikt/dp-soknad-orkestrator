package no.nav.opplysning

internal class Opplysning(private val svar: List<String>, private val beskrivendeId: String) {
    fun svar() = svar

    fun beskrivendeId() = beskrivendeId
}
