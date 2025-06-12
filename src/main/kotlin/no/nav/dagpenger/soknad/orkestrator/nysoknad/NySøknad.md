# Ny søknad

## Skjema i frontend

* Skjemaet er delt opp i seksjoner, feks en seksjon for "Bostedsland", en annen for "Din Situasjon", osv.
* Alle seksjoner, ledetekster, spørsmål, logikk som dekker "hvis bruker har svart dette, så skal neste spørsmål være 
  dette", validering av input ligger i frontend.

## Data i backend

Besvarelser lagres per seksjon i backend, med egne APIer som håndterer CRU-operasjoner. 

### Alternativ 1

Tabell `BOSTEDSLAND` med en kolonne per faktum, en rad per besvarlse for seksjonen.

* `ID`                  # Unik ID for besvarelsen (generert av backend ved lagring av besvarelse)
* `SEKSJON_VERSJON`     # Versjon av seksjonen (verdi kommer fra frontend)
* `SØKNAD_ID`           # Referenase til søknaden (verdi kommer fra frontend, tidligere mottatt fra backend)
* `FERDIG`              # Boolean som sier noe om bruker er ferdig med å fylle ut seksjonen eller ikke
* `BOSTEDSLAND`         # Kolonnenenavn == nøkkel på faktum som besvares, kolonneverdi == brukers besvarelse på
  faktum (verdi defineres av frontend)

Med denne modellen får vi bare en rad i tabellen per besvarelse. Det er ingen måte å indikere at "faktum var i
søknaden, men ble ikke besvart" fordi en kolonne, feks `BOSTEDSLAND`, vil ha verdien `null` både når faktum var
i søknaden og det ikke er besvart, og når faktum var i søknaden. Hvis det skal legges til et nytt faktum, så 
må både frontend (som kontrollerer søknaden) og backend (som sitter på data om søknaden) endres. Dersom et 
faktum fjernes fra seksjonen, kan tilhørende kolonne ikke fjernes fra tabellen fordi vi må spare på historiske 
besvarelser på tidligere versjoner av seksjonen. Store seksjoner får tabeller med mange kolonner. Er dette en
potensiell utfordring?

#### API

`GET /api/v1/{SØKNAD_ID}/BOSTEDSLAND` 
`PUT /api/v1/{SØKNAD_ID}/BOSTEDSLAND`

GET response og PUT body:

```json
{
   "seksjonVersjon": 1,
   "søknadId": "a052eda4-c517-41ea-bf43-8a4c8f2a7ae5",
   "BOSTEDSLAND": "NOR"
}
```

Det kan hende at denne JSON-strukturen er litt krøkkete å få til fordi vi må hente ut kolonnenavnet å bruke den som key.

Alternativ struktur kan være (men her må vi også hente ut kolonnenavnet for bruk i JSON-strukturen):

```json
{
   "seksjonVersjon": 1,
   "søknadId": "a052eda4-c517-41ea-bf43-8a4c8f2a7ae5",
   "fakta": [
      {
         "faktumNøkkel": "BOSTEDSLAND",
         "svar": "NOR"
      }
   ]
}
```

### Alternativ 2

Tabell `BOSTEDSLAND` med en rad per besvart faktum for seksjonen.

* `ID`                  # Unik ID for besvarelsen (generert av backend ved lagring av besvarelse)
* `SEKSJON_VERSJON`     # Versjon av seksjonen (verdi kommer fra frontend)
* `SØKNAD_ID`           # Referenase til søknaden (verdi kommer fra frontend, tidligere mottatt fra backend)
* `FAKTUM_TYPE`         # Type på faktum som besvares (verdi kommer fra frontend, feks `LISTE<BARN>`, `DATO`)
* `FAKTUM_NØKKEL`       # Nøkkel på faktum som besvares (verdi kommer fra frontend)
* `FAKTUM_VERDI`        # Brukers besvarelse på faktum (verdi kommer fra frontend)

Med denne modellen får vi en rad i tabellen per faktum som besvares. Vi kan indikere
at "faktum var i søknaden, men ble ikke besvart" med at det lages en rad hvor `FAKTUM_VERDI` == `null`. Ved endring i 
søknaden, er det ikke nødvendig å gjøre endringer i databasemodellen, med mindre det gjøres endringer på 
seksjons-nivå (ny, slett, splitt, slå sammen). `FAKTUM_NØKKEL` heter ikke `FAKTUM_ID`, fordi `ID` indikerer at det 
eksistere en `FAKTUM`-tabell i modellen, og det gjør det ikke.

Svar består av komplekse typer, feks `BARN` og `ARBEIDSFORHOLD`, lagres som JSON i `FAKTUM_VERDI`-kolonnen. `JsonPath` 
eller lignende brukes for å hente ut ønskede verdier fra JSON-strukturen. Ved å gjøre dette fremfor å lage 
domeneklasser i Kotlin, så trenger vi minimal håndtering av forskjellige versjoner av den komplekse koden i backend. 
Det kan feks hende at vi ikke kan svare ut et behov på en eldre versjon av `ARBEIDSFORHOLD` fordi verdien knyttet 
til behovet ikke eksisterer, og det må håndteres i backend. Primitive typer kan castes til riktig Kotlin-type gitt
verdien fra `FAKTUM_TYPE`.

#### API

`GET /api/v1/{SØKNAD_ID}/BOSTEDSLAND`  
`PUT /api/v1/{SØKNAD_ID}/BOSTEDSLAND`

GET response og PUT body:

```json
{
   "seksjonVersjon": 1,
   "søknadId": "a052eda4-c517-41ea-bf43-8a4c8f2a7ae5",
   "fakta": [
      {
         "nøkkel": "BOSTEDSLAND",
         "type": "TEKST",
         "svar": "NOR"
      },
      {
         "nøkkel": "ARBEIDSFORHOLD",
         "type": "LISTE<ARBEIDSFORHOLD>",
         "svar": [
            {
               "id": "a2f53337-47cb-42bc-a0da-e82ba759b320",
               "navn": "FRISKE PØLSER AS",
               "land": "NOR",
               "sluttårsak": "OPPSAGT"
            }
         ]
      }
   ]
}
```

### Alternativ 3

Tabell `SEKSJON` med en rad per besvart faktum per seksjon.

* `ID`                  # Unik ID for besvarelsen (generert av backend ved lagring av besvarelse)
* `SØKNAD_ID`           # Referenase til søknaden (verdi kommer fra frontend, tidligere mottatt fra backend)
* `SEKSJON_FRONTEND_ID` # Unik ID på seksjonen (verdi kommer fra frontend, feks `BOSTEDSLAND`)
* `SEKSJON_VERSJON`     # Versjon av besvarelse (verdi kommer fra frontend)
* `FAKTUM_TYPE`         # Type på faktum som besvares (verdi kommer fra frontend, feks `LISTE<BARN>`, `DATO`)
* `FAKTUM_NØKKEL`       # Nøkkel på faktum som besvares (verdi kommer fra frontend)
* `FAKTUM_VERDI`        # Brukers besvarelse på faktum (verdi kommer fra frontend)

Med denne modellen trenger vi bare en tabell for alle seksjoner, faktum, og besvarelser. Ved
endringer i søknaden, også ved endringer på seksjons-nivå, er det ikke nødvendig med endringer i databasemodellen.
`SEKSJON_FRONTEND_ID` kan være en ULID generert av frontend, men en tekstlig ID vil gjøre det lettere å manuelt
navigere i data i databasen, på samme måte som for `FAKTUM_NØKKEL`. `FAKTUM_NØKKEL` heter ikke `FAKTUM_ID`, fordi `ID`
indikerer at det eksistere en `FAKTUM`-tabell i modellen, og det gjør det ikke. Vi kan indikere at "faktum var i 
søknaden, men ble ikke besvart" med at det lages en rad hvor `FAKTUM_VERDI` == `null`.

Svar består av komplekse typer, feks `BARN` og `ARBEIDSFORHOLD`, lagres som JSON i `FAKTUM_VERDI`-kolonnen. `JsonPath`
eller lignende brukes for å hente ut ønskede verdier fra JSON-strukturen. Ved å gjøre dette fremfor å lage
domeneklasser i Kotlin, så trenger vi minimal håndtering av forskjellige versjoner av den komplekse koden i backend.
Det kan feks hende at vi ikke kan svare ut et behov på en eldre versjon av `ARBEIDSFORHOLD` fordi verdien knyttet
til behovet ikke eksisterer, og det må håndteres i backend. Primitive typer kan castes til riktig Kotlin-type gitt
verdien fra `FAKTUM_TYPE`.

#### API

`GET /api/v1/{SØKNAD_ID}/{SEKSJON_FRONTEND_ID}`  
`PUT /api/v1/{SØKNAD_ID}/{SEKSJON_FRONTEND_ID}`

GET response og PUT body:

```json
{
   "seksjonVersjon": 1,
   "søknadId": "a052eda4-c517-41ea-bf43-8a4c8f2a7ae5",
   "seksjonFrontendId": "BOSTEDSLAND",
   "fakta": [
      {
         "nøkkel": "BOSTEDSLAND",
         "svar": "NOR",
         "type": "TEKST"
      },
      {
         "nøkkel": "ARBEIDSFORHOLD",
         "type": "LISTE<ARBEIDSFORHOLD>",
         "svar": [
            {
               "id": "a2f53337-47cb-42bc-a0da-e82ba759b320",
               "navn": "FRISKE PØLSER AS",
               "land": "NOR",
               "sluttårsak": "OPPSAGT"
            }
         ]
      }
   ]
}
```

### Diverse

Med modellene i [Alternativ 2](#alternativ-2) og [Alternativ 3](#alternativ-3) har vi
ikke noen enkel måte å indikere at en bruker er ferdig med å fylle ut en søknad, siden alle spørsmålene for en
søknad er fordelt på flere rader i tabellen. Enten må frontend selv utlede om en bruker er ferdig gitt data fra
backend, eller så må backend lagre den informasjonen i en annen tabell. Utleding når en bruker er i en gitt seksjon 
bør være ganske greit, men på oppsummeringssiden kan det potensielt bli litt knot. Men dette kan nok en frontender 
avklare.

Litt ekstra om `SEKSJON_VERSJON`: Denne verdien er det frontend som genererer og eier, og sender til backend. Tanken 
bak det er at dersom frontend gjør endringer i en seksjon, så får den også en ny ID. Når frontend mottar data som
tilhører en gitt seksjon fra backend, så kan frontend sjekke om de dataene som er mottatt har `SEKSJON_VERSJON`
lik versjonen av seksjonen i frontend. Dersom dette ikke er tilfelle, kan frontend be bruker om å fylle ut
seksjonen på nytt. Se også punkt (2) i [Avklaringer](#avklaringer) under.

APIet validerer all input (er det faktisk en dato det som sendes inn som type `DATO`), selv om validering av 
brukerinput er gjort av frontend. APIet skal ikke stole blindt på at frontend gjør valideringene korrekt, og vi 
trenger en ekstra sanity check for å verifisere at vi ikke setter inn data som vi ikke klarer å få ut igjen. En dato 
som ikke er en dato vil feks feile ved uthenting.

## Avklaringer

1. Er det nødvendig å lagre alle mulige alternativer, også de som ikke ble valg? Eksempler er alle land som kunne 
   velges som bostedsland og "NO" ble valgt, og at et spørsmål hadde alternativene "Ja", "Nei", og "Kanskje", og "Nei" 
   ble valgt. Disse kan f.eks. lagres i en kolonne FAKTUM_ALTERNATIVER i tabellen hvor brukers besvarelse er lagret. 
   Dersom det er nødvendig å lagre alle alternativer som var tilgjengelig da bruker søkte, så blir 
   [Alternativ 1](#alternativ-1) mindre aktuell fordi antall kolonner som trengs omtrent dobles.
2. Er det OK for fag og UX at en bruker, som ikke har sendt inn søknad, kan bli bedt om å fylle ut _alle_ data for 
   en gitt seksjon på nytt dersom seksjonen har endret seg i tidsrommet fra bruker har opprettet søknaden til den er 
   sendt inn. Bruker kan f.eks. få beskjed om dette i det bruker skal fortsette på en søknad hen har jobbet på 
   tidligere, og på oppsummeringssiden.
3. For visning av innsendt søknad, er det tilstrekkelig at vi viser PDF-versjonen, og ikke selve skjeamet?

Merk at dersom 2 og 3 er OK, så letter det utviklingsjobben ved endring av søknad veldig fordi vi ikke trenger å 
migrere søknader som ikke er innsendt og/eller gjøre endringer i APIet (expand-extract) for å støtte flere versjoner 
av søknaden. Hverken frontend eller backend trenger å håndtere flere versjoner av søknaden samtidig. Se
[beskrivende scenarier](#scenarier-som-må-løses-i-modellen-skissert-over) for eksempler på hvordan forskjellige 
endringer i søknaden håndteres.

## Scenarier som må løses i modellen skissert over

### Ledetekst i en seksjon endres

1. En ledetekst i en seksjon endres (i frontend, som eier søknaden).
2. `seksjonVersjon` endres i frontend.
3. Frontend deployes til produksjon.
4. Søker gjør en av følgende operasjoner på søknaden:
   1. Åpner en tidligere påbegynt søknad på nytt.
   2. Navigerer i søknaden til seksjonen som er endret.
   3. Eller navigerer til oppsummeringssiden.
5. I alle tre tilfeller i punkt (4) over vil frontend gjøre et kall til backend for å få data om hva bruker har 
   svart for gitt seksjon. For (4.3) vil frontend be om data for alle seksjoner.
6. Dersom backend ikke returnerer noe data for gitt seksjon, så har ikke bruker svart på noen av spørsmålene i 
   seksjonen, og frontend gjør ikke bruker oppmerksom på at det er gjort endringer.
7. Dersom backend returnerer data med en `seksjonVersjon` ulik den frontend nå bruker, så gis bruker beskjed om at 
   seksjonen må fylles ut på nytt.

### Et spørsmål får et nytt alternativ.

### Den neste hendelsen etter at man har svart på et gitt alternativ endres

(f.eks. at et annet spørsmål enn tidligere trigges)

### Et spørsmål fjernes helt.

### Et spørsmål legges til.

### En seksjon fjernes helt.

### En seksjon splittes i to eller flere seksjoner.

### En ny seksjon legges til.
