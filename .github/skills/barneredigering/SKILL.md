---
name: barneredigering
description: |
  Historiske barnedata fra saksbehandlers tidligere barneredigering i dp-soknad-orkestrator.
  Bruk denne skillen når du jobber med barn i søknader: tabellen saksbehandler_barn, barn-behovløserne
  eller barn_soknad_mapping. Skriveflyten er avviklet, bare lesing er igjen.
---

# Barn og saksbehandler_barn

Sist verifisert mot koden: oktober 2026.

## Status

Saksbehandlers redigering av barn er avviklet i denne appen. dp-saksbehandling-frontend lagrer barn
direkte mot dp-behandling. Appen har ikke lenger endepunkter, service-metoder, repository-metoder
eller HTTP-kall mot dp-behandling for å lese, endre, legge til eller slette barn i `saksbehandler_barn`.

Databasen er uendret. Tabellen `saksbehandler_barn` beholdes som historikk, og behovløserne leser
den fortsatt. Ikke legg til ny skrivekode mot tabellen uten en eksplisitt beslutning om det.

## Leserekkefølge

Behovløserne `BarnetilleggBehovLøser` og `BarnetilleggV2BehovLøser` sjekker kildene i denne
rekkefølgen:

1. **`saksbehandler_barn`**: siste snapshot per søknad. Finnes det en rad, er den fasit.
2. **`quiz_opplysning`-tabellene**: gammel søknad. Registerbarn (`faktum.register.barn-liste`) og
   egne barn (`faktum.barn-liste`).
3. **`seksjon_v2`**: ny søknad. Barnetillegg-seksjonen har JSON med listene `barnFraPdl` og
   `barnLagtManuelt`.

Et lagret snapshot skygger for de andre kildene. Et tomt snapshot (`[]`) betyr at søknaden ikke har
barn, og stopper videre oppslag. Bare `null` fra `saksbehandlerBarnRepository.hentBarn()` betyr at
neste kilde skal prøves. Ikke endre denne forskjellen: den bestemmer hva dp-behandling får vite om
eldre søknader.

## Tabellen saksbehandler_barn (append-only)

Hver rad lagrer **hele barnelista** som JSON. Raden er aldri oppdatert. Siste rad per søknad
(sortert på `opprettet DESC, id DESC`) er gjeldende tilstand, og tidligere rader er historikk.

```sql
CREATE TABLE saksbehandler_barn (
    id        BIGSERIAL PRIMARY KEY,
    soknad_id UUID NOT NULL REFERENCES soknad(soknad_id),
    barn      JSON NOT NULL,         -- Hele List<BarnSvar> som JSON-array
    endret_av TEXT NOT NULL,         -- Saksbehandler-ID
    opprettet TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
```

Migrasjonen ligger i `src/main/resources/db/migration/V22__CREATE_TABLE_SAKSBEHANDLER_BARN.sql`.
Kolonnen bruker `JSON` (ikke `JSONB`) for å være lik `seksjon_v2` og bevare rekkefølgen på nøklene.

## Nøkkelfiler

- `src/main/kotlin/.../opplysning/SaksbehandlerBarnRepository.kt`: interface med bare
  `hentBarn(søknadId): List<BarnSvar>?`.
- `src/main/kotlin/.../opplysning/SaksbehandlerBarnRepositoryPostgres.kt`: Exposed-implementasjon
  som leser siste rad.
- `src/main/kotlin/.../opplysning/SaksbehandlerBarnTabell.kt`: Exposed-tabellobjektet.
- `src/main/kotlin/.../quizOpplysning/db/QuizOpplysningRepository.kt`: leser barn fra gamle søknader og
  har `lagreBarnSøknadMapping` og `hentEllerOpprettSøknadbarnId`.
- `src/main/kotlin/.../quizOpplysning/db/Tabeller.kt`: `BarnSøknadMappingTabell`
  (`barn_soknad_mapping`), som kobler `søknadbarnId` til `søknadId`. Behovløserne og
  `SøknadRepository` oppretter mappingen.
- `src/main/kotlin/.../behov/løsere/BarnetilleggV2BehovLøser.kt`: løser behovet `BarnetilleggV2`.
  Inneholder `BarnetilleggV2Løsning` og `LøsningsbarnV2`.
- `src/main/kotlin/.../behov/løsere/BarnetilleggBehovLøser.kt`: gammel V1-løser. Har
  `BESKRIVENDE_ID_PDL_BARN` og `BESKRIVENDE_ID_EGNE_BARN` i companion object.
- `src/main/kotlin/.../quizOpplysning/datatyper/Barn.kt`: `BarnSvar` og
  `Barn.barnetilleggperiode(fødselsdato)`, som returnerer `fødselsdato to fødselsdato + 18 år`.
- `src/main/kotlin/.../barn/BarnApi.kt`: `GET /barn` (TokenX). Dette er søkerens eget oppslag mot PDL,
  ikke den avviklede saksbehandlerflyten.

`ApplicationBuilder.kt` sender `saksbehandlerBarnRepository` til `BehovløserFactory`.

## Datatyper

- `barnSvarId` identifiserer ett barn. `søknadbarnId` i løsningen identifiserer koblingen til søknaden,
  ikke et enkelt barn.
- `fraRegister` angir om barnet kommer fra PDL. `endretAv` angir saksbehandleren som endret barnet.
- `BarnSvar.kvalifisererTilBarnetillegg` mappes til `LøsningsbarnV2.kvalifiserer`. Perioden,
  `endretAv` og `begrunnelse` følger med i løsningen.
- Begrunnelsen ligger per barn (inne i JSON), ikke per snapshot.

## Barn i seksjon_v2

```json
{
  "barnFraPdl": [
    {
      "id": "uuid",
      "fornavnOgMellomnavn": "...",
      "etternavn": "...",
      "fødselsdato": "2013-05-26",
      "bostedsland": "NOR",
      "forsørgerDuBarnet": "ja"
    }
  ],
  "barnLagtManuelt": [
    {
      "id": "uuid",
      "fornavnOgMellomnavn": "...",
      "etternavn": "...",
      "fødselsdato": "2025-10-21",
      "bostedsland": "ARG"
    }
  ]
}
```

Elementer i `barnLagtManuelt` kan mangle `forsørgerDuBarnet`. Feltet `id` brukes som `barnSvarId`,
og `bostedsland` mappes til `statsborgerskap`. Når barnet kvalifiserer, beregnes `barnetilleggFom` og
`barnetilleggTom` med `Barn.barnetilleggperiode(fødselsdato)`. Ellers er de null.

## Testmønstre

Testene ligger i `BarnetilleggBehovLøserTest` og `BarnetilleggV2BehovLøserTest`. De bruker
`InMemoryQuizOpplysningRepository` og en mockk for `SaksbehandlerBarnRepository`:

```kotlin
val saksbehandlerBarnRepository =
    mockk<SaksbehandlerBarnRepository>(relaxed = true).also {
        every { it.hentBarn(any()) } returns null  // Standard: ingen snapshot
    }
```

Overstyr per test for å dekke at et snapshot har forrang, og at et tomt snapshot gir ingen barn.
