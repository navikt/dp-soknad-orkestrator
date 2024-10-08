package no.nav.dagpenger.soknad.orkestrator.opplysning.seksjoner

import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysningsbehov
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysningstype
import no.nav.dagpenger.soknad.orkestrator.opplysning.Svar

object Bostedsland : Seksjon() {
    override val navn = Seksjonsnavn.BOSTEDSLAND
    override val versjon = "BOSTEDSLAND_V1"

    val hvilketLandBorDuI =
        Opplysningsbehov(
            id = 1,
            tekstnøkkel = "faktum.hvilket-land-bor-du-i",
            type = Opplysningstype.LAND,
            gyldigeSvar = listOf("NOR", "SWE", "FIN"),
        )

    val reistTilbakeTilNorge =
        Opplysningsbehov(
            id = 2,
            tekstnøkkel = "faktum.reist-tilbake-etter-arbeidsledig",
            type = Opplysningstype.BOOLEAN,
        )

    val datoForAvreise =
        Opplysningsbehov(
            id = 3,
            tekstnøkkel = "faktum.reist-tilbake-periode",
            type = Opplysningstype.PERIODE,
        )

    val hvorforReisteFraNorge =
        Opplysningsbehov(
            id = 4,
            tekstnøkkel = "faktum.reist-tilbake-aarsak",
            type = Opplysningstype.TEKST,
        )

    val enGangIUken =
        Opplysningsbehov(
            id = 5,
            tekstnøkkel = "faktum.reist-tilbake-en-gang-eller-mer",
            type = Opplysningstype.BOOLEAN,
        )

    val rotasjon =
        Opplysningsbehov(
            id = 6,
            tekstnøkkel = "faktum.reist-i-takt-med-rotasjon",
            type = Opplysningstype.BOOLEAN,
        )

    override fun førsteOpplysningsbehov(): Opplysningsbehov = hvilketLandBorDuI

    override fun nesteOpplysningsbehov(
        svar: Svar<*>,
        opplysningsbehovId: Int,
    ): Opplysningsbehov? {
        return when (opplysningsbehovId) {
            hvilketLandBorDuI.id -> if (svar.verdi != "NOR") reistTilbakeTilNorge else null
            reistTilbakeTilNorge.id -> if (svar.verdi == true) datoForAvreise else enGangIUken
            datoForAvreise.id -> hvorforReisteFraNorge
            hvorforReisteFraNorge.id -> enGangIUken
            enGangIUken.id -> if (svar.verdi == true) null else rotasjon
            rotasjon.id -> null
            else -> null
        }
    }

    override fun getOpplysningsbehov(opplysningsbehovId: Int): Opplysningsbehov =
        when (opplysningsbehovId) {
            hvilketLandBorDuI.id -> hvilketLandBorDuI
            reistTilbakeTilNorge.id -> reistTilbakeTilNorge
            datoForAvreise.id -> datoForAvreise
            hvorforReisteFraNorge.id -> hvorforReisteFraNorge
            enGangIUken.id -> enGangIUken
            rotasjon.id -> rotasjon
            else -> throw IllegalArgumentException("Ukjent opplysning med id: $opplysningsbehovId")
        }

    override fun avhengigheter(opplysningsbehovId: Int): List<Int> =
        when (opplysningsbehovId) {
            hvilketLandBorDuI.id ->
                listOf(
                    reistTilbakeTilNorge.id,
                    datoForAvreise.id,
                    hvorforReisteFraNorge.id,
                    enGangIUken.id,
                    rotasjon.id,
                )

            reistTilbakeTilNorge.id -> listOf(datoForAvreise.id, hvorforReisteFraNorge.id)
            datoForAvreise.id -> listOf(hvorforReisteFraNorge.id)
            hvorforReisteFraNorge.id -> emptyList()
            enGangIUken.id -> listOf(rotasjon.id)
            rotasjon.id -> emptyList()
            else -> throw IllegalArgumentException("Ukjent opplysning med opplysningId: $opplysningsbehovId")
        }

    override fun validerSvar(
        opplysningsbehovId: Int,
        svar: Svar<*>,
    ) {
        if (opplysningsbehovId == hvilketLandBorDuI.id) {
            if (hvilketLandBorDuI.gyldigeSvar?.contains(svar.verdi) != true) {
                throw IllegalArgumentException("$svar er ikke et gyldig svar")
            }
        }
    }
}
