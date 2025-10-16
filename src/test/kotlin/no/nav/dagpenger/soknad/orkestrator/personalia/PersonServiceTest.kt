package no.nav.dagpenger.soknad.orkestrator.personalia

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.pdl.PersonOppslag
import no.nav.dagpenger.soknad.orkestrator.utils.PdlTestUtil
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PersonServiceTest {
    @Test
    fun `hentPerson returnerer forventet respons hvis PDL returnerer en person`() {
        val personOppslag = mockk<PersonOppslag>()
        val personService = PersonService(personOppslag) { "token" }
        coEvery { personOppslag.hentPerson(any(), any()) } returns
            PdlTestUtil().lagPdlPerson(
                "OLA",
                "PETTER",
                "DUNK",
                LocalDate.of(2020, 1, 1),
                71,
                "30212018224",
            )

        runBlocking {
            val person = personService.hentPerson("27279320064", "subjectToken")

            person.fornavn shouldBe "OLA"
            person.mellomnavn shouldBe "PETTER"
            person.etternavn shouldBe "DUNK"
            person.fodselsDato shouldBe LocalDate.of(2020, 1, 1)
            person.alder shouldBe 71
            person.ident shouldBe "30212018224"
        }
    }
}
