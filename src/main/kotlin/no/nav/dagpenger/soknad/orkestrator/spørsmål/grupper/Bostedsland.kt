package no.nav.dagpenger.soknad.orkestrator.spørsmål.grupper

import no.nav.dagpenger.soknad.orkestrator.api.models.SporsmalDTO
import no.nav.dagpenger.soknad.orkestrator.api.models.SporsmalTypeDTO
import no.nav.dagpenger.soknad.orkestrator.spørsmål.SpørsmålType
import no.nav.dagpenger.soknad.orkestrator.spørsmål.Svar
import java.util.UUID

data class GrunnleggendeSpørsmål(
    val idIGruppe: Int,
    val tekstnøkkel: String,
    val type: SpørsmålType,
    val gyldigeSvar: List<String>,
)

fun GrunnleggendeSpørsmål.toSporsmalDTO(
    spørsmålId: UUID,
    svar: String?,
): SporsmalDTO =
    SporsmalDTO(
        id = spørsmålId,
        tekstnøkkel = tekstnøkkel,
        type = SporsmalTypeDTO.valueOf(type.name),
        svar = svar,
        gyldigeSvar = gyldigeSvar,
    )

abstract class Bostedsland {
    abstract val versjon: Int
    abstract val navn: Spørsmålgruppe

    abstract fun førsteSpørsmål(): GrunnleggendeSpørsmål

    abstract fun nesteSpørsmål(
        spørsmålId: Int,
        svar: Svar<*>,
    ): GrunnleggendeSpørsmål?

    abstract fun getSpørsmålMedId(id: Int): GrunnleggendeSpørsmål

    abstract fun avhengigheter(id: Int): List<Int>

    abstract fun valider(
        spørsmålId: Int,
        svar: Svar<*>,
    )
}

object BostedslandDTOV1 : Bostedsland() {
    override val versjon = 1
    override val navn = Spørsmålgruppe.BOSTEDSLAND

    val hvilketLandBorDuI =
        GrunnleggendeSpørsmål(
            idIGruppe = 1,
            tekstnøkkel = "bostedsland.hvilket-land-bor-du-i",
            type = SpørsmålType.LAND,
            gyldigeSvar = listOf("NOR", "SWE", "FIN"),
        )

    val reistTilbakeTilNorge =
        GrunnleggendeSpørsmål(
            idIGruppe = 2,
            tekstnøkkel = "bostedsland.reist-tilbake-til-norge",
            type = SpørsmålType.BOOLEAN,
            gyldigeSvar = emptyList(),
        )

    val datoForAvreise =
        GrunnleggendeSpørsmål(
            idIGruppe = 3,
            tekstnøkkel = "bostedsland.dato-for-avreise",
            type = SpørsmålType.PERIODE,
            gyldigeSvar = emptyList(),
        )

    val hvorforReisteFraNorge =
        GrunnleggendeSpørsmål(
            idIGruppe = 4,
            tekstnøkkel = "bostedsland.hvorfor",
            type = SpørsmålType.TEKST,
            gyldigeSvar = emptyList(),
        )

    val enGangIUken =
        GrunnleggendeSpørsmål(
            idIGruppe = 5,
            tekstnøkkel = "bostedsland.en-gang-i-uken",
            type = SpørsmålType.BOOLEAN,
            gyldigeSvar = emptyList(),
        )

    val rotasjon =
        GrunnleggendeSpørsmål(
            idIGruppe = 6,
            tekstnøkkel = "bostedsland.rotasjon",
            type = SpørsmålType.BOOLEAN,
            gyldigeSvar = emptyList(),
        )

    override fun førsteSpørsmål(): GrunnleggendeSpørsmål = hvilketLandBorDuI

    override fun nesteSpørsmål(
        spørsmålId: Int,
        svar: Svar<*>,
    ): GrunnleggendeSpørsmål? =
        when (spørsmålId) {
            hvilketLandBorDuI.idIGruppe -> if (svar.verdi != "NOR") reistTilbakeTilNorge else null
            reistTilbakeTilNorge.idIGruppe -> if (svar.verdi == true) datoForAvreise else enGangIUken
            datoForAvreise.idIGruppe -> hvorforReisteFraNorge
            hvorforReisteFraNorge.idIGruppe -> enGangIUken
            enGangIUken.idIGruppe -> if (svar.verdi == true) null else rotasjon
            rotasjon.idIGruppe -> null
            else -> null
        }

    override fun getSpørsmålMedId(id: Int): GrunnleggendeSpørsmål =
        when (id) {
            hvilketLandBorDuI.idIGruppe -> hvilketLandBorDuI
            reistTilbakeTilNorge.idIGruppe -> reistTilbakeTilNorge
            datoForAvreise.idIGruppe -> datoForAvreise
            hvorforReisteFraNorge.idIGruppe -> hvorforReisteFraNorge
            enGangIUken.idIGruppe -> enGangIUken
            rotasjon.idIGruppe -> rotasjon
            else -> throw IllegalArgumentException("Ukjent spørsmål med id: $id")
        }

    override fun avhengigheter(id: Int): List<Int> =
        when (id) {
            hvilketLandBorDuI.idIGruppe ->
                listOf(
                    reistTilbakeTilNorge.idIGruppe,
                    datoForAvreise.idIGruppe,
                    hvorforReisteFraNorge.idIGruppe,
                    enGangIUken.idIGruppe,
                    rotasjon.idIGruppe,
                )
            reistTilbakeTilNorge.idIGruppe -> listOf(datoForAvreise.idIGruppe, hvorforReisteFraNorge.idIGruppe)
            datoForAvreise.idIGruppe -> listOf(hvorforReisteFraNorge.idIGruppe)
            hvorforReisteFraNorge.idIGruppe -> emptyList()
            enGangIUken.idIGruppe -> listOf(rotasjon.idIGruppe)
            rotasjon.idIGruppe -> emptyList()
            else -> throw IllegalArgumentException("Ukjent spørsmål med id: $id")
        }

    override fun valider(
        spørsmålId: Int,
        svar: Svar<*>,
    ) {
        if (spørsmålId == hvilketLandBorDuI.idIGruppe) {
            if (svar.verdi !in hvilketLandBorDuI.gyldigeSvar) {
                throw IllegalArgumentException("$svar er ikke et gyldig svar")
            }
        }
    }
}

fun getGruppe(id: Int): Bostedsland {
    when (id) {
        BostedslandDTOV1.versjon -> return BostedslandDTOV1
        else -> throw IllegalArgumentException("Ukjent gruppe med id: $id")
    }
}
