package no.nav.dagpenger.soknad.orkestrator.spørsmål.grupper

import no.nav.dagpenger.soknad.orkestrator.spørsmål.GrunnleggendeSpørsmål
import no.nav.dagpenger.soknad.orkestrator.spørsmål.SpørsmålType
import no.nav.dagpenger.soknad.orkestrator.spørsmål.Svar

object Bostedsland : Spørsmålgruppe() {
    override val navn = Spørsmålgruppenavn.BOSTEDSLAND

    val hvilketLandBorDuI =
        GrunnleggendeSpørsmål(
            id = 1,
            tekstnøkkel = "faktum.hvilket-land-bor-du-i",
            type = SpørsmålType.LAND,
            gyldigeSvar = listOf("NOR", "SWE", "FIN"),
        )

    val reistTilbakeTilNorge =
        GrunnleggendeSpørsmål(
            id = 2,
            tekstnøkkel = "faktum.reist-tilbake-etter-arbeidsledig",
            type = SpørsmålType.BOOLEAN,
            gyldigeSvar = emptyList(),
        )

    val datoForAvreise =
        GrunnleggendeSpørsmål(
            id = 3,
            tekstnøkkel = "faktum.reist-tilbake-periode",
            type = SpørsmålType.PERIODE,
            gyldigeSvar = emptyList(),
        )

    val hvorforReisteFraNorge =
        GrunnleggendeSpørsmål(
            id = 4,
            tekstnøkkel = "faktum.reist-tilbake-aarsak",
            type = SpørsmålType.TEKST,
            gyldigeSvar = emptyList(),
        )

    val enGangIUken =
        GrunnleggendeSpørsmål(
            id = 5,
            tekstnøkkel = "faktum.reist-tilbake-en-gang-eller-mer",
            type = SpørsmålType.BOOLEAN,
            gyldigeSvar = emptyList(),
        )

    val rotasjon =
        GrunnleggendeSpørsmål(
            id = 6,
            tekstnøkkel = "faktum.reist-i-takt-med-rotasjon",
            type = SpørsmålType.BOOLEAN,
            gyldigeSvar = emptyList(),
        )

    override fun førsteSpørsmål(): GrunnleggendeSpørsmål = hvilketLandBorDuI

    override fun nesteSpørsmål(
        svar: Svar<*>,
        gruppespørsmålId: Int,
    ): GrunnleggendeSpørsmål? {
        return when (gruppespørsmålId) {
            hvilketLandBorDuI.id -> if (svar.verdi != "NOR") reistTilbakeTilNorge else null
            reistTilbakeTilNorge.id -> if (svar.verdi == true) datoForAvreise else enGangIUken
            datoForAvreise.id -> hvorforReisteFraNorge
            hvorforReisteFraNorge.id -> enGangIUken
            enGangIUken.id -> if (svar.verdi == true) null else rotasjon
            rotasjon.id -> null
            else -> null
        }
    }

    override fun getSpørsmål(spørsmålId: Int): GrunnleggendeSpørsmål =
        when (spørsmålId) {
            hvilketLandBorDuI.id -> hvilketLandBorDuI
            reistTilbakeTilNorge.id -> reistTilbakeTilNorge
            datoForAvreise.id -> datoForAvreise
            hvorforReisteFraNorge.id -> hvorforReisteFraNorge
            enGangIUken.id -> enGangIUken
            rotasjon.id -> rotasjon
            else -> throw IllegalArgumentException("Ukjent spørsmål med id: $spørsmålId")
        }

    override fun avhengigheter(spørsmålId: Int): List<Int> =
        when (spørsmålId) {
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
            else -> throw IllegalArgumentException("Ukjent spørsmål med spørsmålId: $spørsmålId")
        }

    override fun validerSvar(
        spørsmålId: Int,
        svar: Svar<*>,
    ) {
        if (spørsmålId == hvilketLandBorDuI.id) {
            if (svar.verdi !in hvilketLandBorDuI.gyldigeSvar) {
                throw IllegalArgumentException("$svar er ikke et gyldig svar")
            }
        }
    }
}
