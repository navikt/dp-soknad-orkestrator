package no.nav.dagpenger.soknad.orkestrator.spørsmål.grupper

import no.nav.dagpenger.soknad.orkestrator.spørsmål.GrunnleggendeSpørsmål
import no.nav.dagpenger.soknad.orkestrator.spørsmål.Svar

abstract class Spørsmålgruppe {
    abstract val navn: Spørsmålgruppenavn

    abstract fun førsteSpørsmål(): GrunnleggendeSpørsmål

    abstract fun nesteSpørsmål(
        svar: Svar<*>,
        gruppespørsmålId: Int,
    ): GrunnleggendeSpørsmål?

    abstract fun getSpørsmål(spørsmålId: Int): GrunnleggendeSpørsmål

    abstract fun avhengigheter(spørsmålId: Int): List<Int>

    abstract fun validerSvar(
        spørsmålId: Int,
        svar: Svar<*>,
    )
}

fun getSpørsmålgruppe(gruppenavn: Spørsmålgruppenavn): Spørsmålgruppe {
    when (gruppenavn) {
        Bostedsland.navn -> return Bostedsland
        else -> throw IllegalArgumentException("Ukjent gruppe med navn: $gruppenavn")
    }
}

enum class Spørsmålgruppenavn {
    BOSTEDSLAND,
}
