package no.nav.dagpenger.soknad.orkestrator.personalia

class PersonaliaService(
    val personService: PersonService,
    val kontonummerService: KontonummerService,
) {
    suspend fun getPersonalia(
        fnr: String,
        subjectToken: String,
    ): PersonaliaDto =
        PersonaliaDto(
            person = personService.hentPerson(fnr, subjectToken),
            kontonummer = kontonummerService.hentKontonummer(subjectToken).kontonummer,
        )
}
