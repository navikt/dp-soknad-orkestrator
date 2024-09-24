package no.nav.dagpenger.soknad.orkestrator.opplysning.grupper

import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysningsbehov
import no.nav.dagpenger.soknad.orkestrator.opplysning.Svar

abstract class Seksjon {
    abstract val navn: Seksjonsnavn
    abstract val versjon: String

    abstract fun førsteOpplysningsbehov(): Opplysningsbehov

    abstract fun nesteOpplysningsbehov(
        svar: Svar<*>,
        opplysningsbehovId: Int,
    ): Opplysningsbehov?

    abstract fun getOpplysningsbehov(opplysningsbehovId: Int): Opplysningsbehov

    abstract fun avhengigheter(opplysningsbehovId: Int): List<Int>

    abstract fun validerSvar(
        opplysningsbehovId: Int,
        svar: Svar<*>,
    )
}

fun getSeksjon(navn: Seksjonsnavn): Seksjon {
    when (navn) {
        Bostedsland.navn -> return Bostedsland
        else -> throw IllegalArgumentException("Ukjent gruppe med navn: $navn")
    }
}

enum class Seksjonsnavn {
    BOSTEDSLAND,
}
