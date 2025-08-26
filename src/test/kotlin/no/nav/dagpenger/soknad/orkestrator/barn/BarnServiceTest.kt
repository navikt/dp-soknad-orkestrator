package no.nav.dagpenger.soknad.orkestrator.barn

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.inspectors.forAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.pdl.PersonOppslagBolk
import no.nav.dagpenger.soknad.orkestrator.utils.PdlTestUtil
import org.junit.jupiter.api.Test
import java.time.LocalDate.now

class BarnServiceTest {
    private val personOppslagBolk = mockk<PersonOppslagBolk>()
    private val barnService = BarnService(personOppslagBolk) { "token" }

    @Test
    fun `hentBarn returnerer forventet respons`() {
        val barnUnder18År =
            PdlTestUtil().lagPdlPerson(
                fornavn = "fornavn1",
                mellomnavn = "mellomnavn1",
                etternavn = "etternavn1",
                fodselsdato = now().minusYears(7),
                fodseslnummer = "24225505906",
            )
        val barnOver18År =
            PdlTestUtil().lagPdlPerson(
                fornavn = "fornavn1",
                mellomnavn = "mellomnavn1",
                etternavn = "etternavn1",
                fodselsdato = now().minusYears(18),
                fodseslnummer = "24225505906",
            )
        coEvery { personOppslagBolk.hentBarn(any(), any()) } returns listOf(barnUnder18År, barnOver18År, barnUnder18År)

        runBlocking {
            val barn = barnService.hentBarn("24225505906")

            barn shouldHaveSize 2
            barn.forAll { barn ->
                barn.fornavn shouldBe barnUnder18År.fornavn
                barn.mellomnavn shouldBe barnUnder18År.mellomnavn
                barn.fornavnOgMellomnavn shouldBe "${barn.fornavn} ${barn.mellomnavn}"
                barn.etternavn shouldBe barnUnder18År.etternavn
                barn.fødselsdato shouldBe barnUnder18År.fodselsdato
                barn.bostedsland shouldBe "XUK"
                barn.alder() shouldBe 7
            }
        }
    }

    @Test
    fun `hentBarn kaster exception hvis kall mot PDL feiler`() {
        coEvery { personOppslagBolk.hentBarn(any(), any()) } throws RuntimeException()

        runBlocking {
            shouldThrow<RuntimeException> {
                barnService.hentBarn("24225505906")
            }
        }
    }
}
