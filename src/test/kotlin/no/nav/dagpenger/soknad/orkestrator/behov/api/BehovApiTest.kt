package no.nav.dagpenger.soknad.orkestrator.behov.api

import io.kotest.matchers.shouldBe
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.jwt
import io.mockk.mockk
import no.nav.dagpenger.soknad.orkestrator.api.auth.AuthFactory.azureAdMaskin
import no.nav.dagpenger.soknad.orkestrator.api.models.BehovRequestDTO
import no.nav.dagpenger.soknad.orkestrator.api.models.BehovResponseDTO
import no.nav.dagpenger.soknad.orkestrator.behov.SynkronBehovløser
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.QuizOpplysning
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Arbeidsforhold
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.ArbeidsforholdSvar
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Sluttårsak
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonRepository
import no.nav.dagpenger.soknad.orkestrator.utils.InMemoryQuizOpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.utils.TestApplication.testAzureADMaskinToken
import no.nav.dagpenger.soknad.orkestrator.utils.TestApplication.testAzureADToken
import no.nav.dagpenger.soknad.orkestrator.utils.TestApplication.withMockAuthServerAndTestApplication
import no.nav.dagpenger.soknad.orkestrator.utils.TestContext
import org.junit.jupiter.api.Test
import java.util.UUID

class BehovApiTest {
    private val opplysningRepository = InMemoryQuizOpplysningRepository()
    private val ident = "12345678910"
    private val søknadId = UUID.randomUUID()

    private val testModul: Application.() -> Unit = {
        install(Authentication) {
            jwt("azureAd-maskin") { azureAdMaskin() }
        }
        behovApi(SynkronBehovløser(opplysningRepository, mockk<SeksjonRepository>(relaxed = true)))
    }

    @Test
    fun `Permittert svarer true for vanlig permittering`() =
        withMockAuthServerAndTestApplication(testModul) {
            lagreArbeidsforhold(Sluttårsak.PERMITTERT)

            val response = postBehov("Permittert")

            response.status shouldBe HttpStatusCode.OK
            response.body<BehovResponseDTO>().verdi shouldBe true
        }

    @Test
    fun `PermittertFiskeforedling svarer true for fiskepermittering`() =
        withMockAuthServerAndTestApplication(testModul) {
            lagreArbeidsforhold(Sluttårsak.PERMITTERT_FISKEFOREDLING)

            postBehov("PermittertFiskeforedling").body<BehovResponseDTO>().verdi shouldBe true
            postBehov("Permittert").body<BehovResponseDTO>().verdi shouldBe false
        }

    @Test
    fun `behov som ikke er implementert synkront gir 400`() =
        withMockAuthServerAndTestApplication(testModul) {
            postBehov("Ordinær").status shouldBe HttpStatusCode.BadRequest
        }

    @Test
    fun `ukjent behov gir 400`() =
        withMockAuthServerAndTestApplication(testModul) {
            postBehov("Finnesikke").status shouldBe HttpStatusCode.BadRequest
        }

    @Test
    fun `manglende body gir 400`() =
        withMockAuthServerAndTestApplication(testModul) {
            client
                .post("/behov/Permittert") {
                    bearerAuth(testAzureADMaskinToken())
                    contentType(ContentType.Application.Json)
                    setBody("{}")
                }.status shouldBe HttpStatusCode.BadRequest
        }

    @Test
    fun `avviser token uten idtyp app`() =
        withMockAuthServerAndTestApplication(testModul) {
            client
                .post("/behov/Permittert") { bearerAuth(testAzureADMaskinToken(claims = emptyMap())) }
                .status shouldBe HttpStatusCode.Unauthorized
        }

    @Test
    fun `avviser saksbehandlertoken`() =
        withMockAuthServerAndTestApplication(testModul) {
            client.post("/behov/Permittert") { bearerAuth(testAzureADToken) }.status shouldBe HttpStatusCode.Unauthorized
        }

    @Test
    fun `avviser kall uten token`() =
        withMockAuthServerAndTestApplication(testModul) {
            client.post("/behov/Permittert").status shouldBe HttpStatusCode.Unauthorized
        }

    private suspend fun TestContext.postBehov(
        behovNavn: String,
        request: BehovRequestDTO = BehovRequestDTO(ident, søknadId),
    ) = client.post("/behov/$behovNavn") {
        bearerAuth(testAzureADMaskinToken())
        contentType(ContentType.Application.Json)
        setBody(request)
    }

    private fun lagreArbeidsforhold(sluttårsak: Sluttårsak) =
        opplysningRepository.lagre(
            QuizOpplysning(
                beskrivendeId = "faktum.arbeidsforhold",
                type = Arbeidsforhold,
                svar = listOf(ArbeidsforholdSvar(navn = "arbeidsforhold", land = "NOR", sluttårsak = sluttårsak)),
                ident = ident,
                søknadId = søknadId,
            ),
        )
}
