package no.nav.dagpenger.soknad.orkestrator.nysoknad

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import java.util.UUID

// Tanken er at vi kan sende en liste med navn på minidialoger/seksjoner, også har frontend en egen komponent for hver seksjon.
// Da er alle felter kodet i frontend, i stedet for at backend skal fortelle frontend hvilke felter som finnes.
// Da må vi endre både i backend og frontend når noe endres, men jeg tror det blir mye mindre/enklere logikk (selv om det er litt mer kode).
// Da vet backend hvilke data som kommer gitt et navn på seksjonen.
//  Vi kan også vurdere et eget endepunkt per seksjon, da slipper vi å håndtere at data kan være flere ting.

// Hvordan skal vi håndtere endringer på seksjonene?
//  Vi har tenkt tidligere at bruker må fylle ut hele seksjonen på én gang, så hvis de ikke er svart på alt, får de ny versjon neste gang de går inn.
//  Da slipper vi å migrere påbegynte seksjoner.
// Men vi må vel likevel tenke på at en seksjon kan være fullført av bruker, men så vil vi endre den.
//  Da vil frontend slite med å vise frem dataene, siden de ikke er i det formatet frontend forventer.
// Vi må også tenke på når noe skal bli en ny seksjon, og når det er en endring på en eksisterende seksjon.
//  Hva gjør vi om vi "arkiverer" en seksjon, fordi den f.eks. deles opp i to; må bruker fylle ut de nye da?

// Kan vi vise bare pdf helt til slutt?
// En seksjon kan endres hvis ikke hele søknaden er sendt inn.
// Frontend må vite hvilken versjon av skjemaet den er på.
//  Da kan man få en advarsel når det kommer lagrede data på forrige versjon. Deretter overskriver man i backend med nye data.
//  Bruker må fylle ut alt på nytt hvis vi lager en ny versjon.

data class Bostedsland(
    val land: String,
    val jobbetUtenForNorge: Boolean,
    val jobbetTurnus: Boolean,
)

data class Arbeidsforhold(
    val arbeidsgivere: List<Arbeidsgiver>,
)

data class Arbeidsgiver(
    val navn: String,
    val organisasjonsnummer: String,
    val sluttårsak: String, // enum?
    val dokumentasjonskrav: String, // Hvordan løser vi dette?
)

enum class Seksjonnavn {
    BOSTEDSLAND,
    ARBEIDSFORHOLD,
}

class Endepunkter {
    // Hvis man allerede har svart på en seksjon, må frontend kunne hente ut dataene for å vise dem.
    //  Skal det være mulig å endre en seksjon etter at man har sendt inn, før hele søknaden er sendt?
    fun hentSeksjonsdata(
        søkandId: UUID,
        seksjonnavn: Seksjonnavn,
    ): String = "greier fra database"

    // Tanken er at databasen kan ha en tabell per seksjon/minidialog.
    //  Da blir det mange kolonner som er nullable
    //  Blir det veldig tungvint når vi skal redigere en seksjon?
    //  Noen av seksjonene kan bli ganske store.
    fun lagreSeksjonsdata(
        søknadId: UUID,
        seksjonnavn: Seksjonnavn,
        data: String,
    ) {
        if (seksjonnavn == Seksjonnavn.BOSTEDSLAND) {
            val bostedslandData = objectMapper.readValue<Bostedsland>(data)
            db.lagreBostedsland(
                søknadId = søknadId,
                bostedslandData,
            )
        } else if (seksjonnavn == Seksjonnavn.ARBEIDSFORHOLD) {
            // lagre Arbeidsforhold
        }
    }

    // En gang i fremtiden vil vi kanskje ha noe sånt, der vi får behov fra regelmotoren, og så oppretter vi seksjoner som må svares på.
    //  Vi trenger kanskje ikke legge til rette for det nå, siden vi ganske lenge vil ha alle seksjoner uansett.
    fun hentPåkrevdeSeksjoner(søkandId: UUID): List<Seksjonnavn> {
        // Hent fra database hvilke seksjoner som er påkrevd for denne søknaden
        return listOf(Seksjonnavn.BOSTEDSLAND, Seksjonnavn.ARBEIDSFORHOLD)
    }
}
