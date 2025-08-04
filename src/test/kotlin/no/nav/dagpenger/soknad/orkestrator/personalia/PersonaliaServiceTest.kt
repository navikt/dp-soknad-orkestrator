package no.nav.dagpenger.soknad.orkestrator.personalia

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.time.LocalDate.now

class PersonaliaServiceTest {
    @Test
    fun `hentPersonalia returnerer forventet respons`() {
        val personService = mockk<PersonService>()
        val kontonummerService = mockk<KontonummerService>()
        val personaliaService = PersonaliaService(personService, kontonummerService)
        val personDto = createPersonDto()
        coEvery { personService.hentPerson(any(), any()) } returns personDto
        coEvery { kontonummerService.hentKontonummer(any()) } returns KontonummerDto("51823888914")

        runBlocking {
            val personalia = personaliaService.hentPersonalia("15230252251", "subjectToken")

            personalia.person shouldBe personDto
            personalia.kontonummer shouldBe "51823888914"
        }
    }

    private fun createPersonDto(): PersonDto =
        PersonDto(
            fornavn = "fornavn",
            mellomnavn = "mellomnavn",
            etternavn = "etternavn",
            fodselsDato = now(),
            ident = "24302435967",
            postAdresse = createAdresseDto("1234"),
            folkeregistrertAdresse = createAdresseDto("1235"),
        )

    private fun createAdresseDto(postnummer: String): AdresseDto =
        AdresseDto(
            adresselinje1 = "adr1",
            adresselinje2 = "adr2",
            adresselinje3 = "adr3",
            postnummer = postnummer,
            poststed = "poststed",
            landkode = "landkode",
            land = "land",
        )
}
