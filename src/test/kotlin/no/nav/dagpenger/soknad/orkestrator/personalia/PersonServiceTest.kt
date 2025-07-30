package no.nav.dagpenger.soknad.orkestrator.personalia

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.pdl.PDLPerson
import no.nav.dagpenger.pdl.PersonOppslag
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PersonServiceTest {
    @Test
    fun `hentPerson returnerer forventet respons hvis PDL returnerer en person`() {
        val personOppslag = mockk<PersonOppslag>()
        val personService = PersonService(personOppslag, { "token" })
        val pdlPerson = mockk<PDLPerson>(relaxed = true)
        every { pdlPerson.fornavn } returns "OLA"
        every { pdlPerson.mellomnavn } returns "PETTER"
        every { pdlPerson.etternavn } returns "DUNK"
        every { pdlPerson.fodselsdato } returns LocalDate.of(2020, 1, 1)
        every { pdlPerson.fodselnummer } returns "30212018224"
        coEvery { personOppslag.hentPerson(any(), any()) } returns pdlPerson

        runBlocking {
            val person = personService.hentPerson("27279320064", "subjectToken")

            person.fornavn shouldBe "OLA"
            person.mellomnavn shouldBe "PETTER"
            person.etternavn shouldBe "DUNK"
            person.fodselsDato shouldBe LocalDate.of(2020, 1, 1)
            person.ident shouldBe "30212018224"
        }
    }
}
