package no.nav.opplysning

internal class Opplysning(private val svar: String, private val beskrivendeId: String) {
    fun svar() = svar

    fun tekstId() = beskrivendeId
}
