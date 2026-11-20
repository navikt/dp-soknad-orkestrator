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
import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.soknad.orkestrator.api.auth.AuthFactory.azureAdMaskin
import no.nav.dagpenger.soknad.orkestrator.api.models.BehovRequestDTO
import no.nav.dagpenger.soknad.orkestrator.api.models.BehovResponseDTO
import no.nav.dagpenger.soknad.orkestrator.behov.BehovløserFactory.Behov
import no.nav.dagpenger.soknad.orkestrator.behov.IkkeStøttetBehov
import no.nav.dagpenger.soknad.orkestrator.behov.SynkronBehovløser
import no.nav.dagpenger.soknad.orkestrator.behov.SøknadFinnesIkke
import no.nav.dagpenger.soknad.orkestrator.utils.TestApplication.testAzureADMaskinToken
import no.nav.dagpenger.soknad.orkestrator.utils.TestApplication.testAzureADToken
import no.nav.dagpenger.soknad.orkestrator.utils.TestApplication.withMockAuthServerAndTestApplication
import no.nav.dagpenger.soknad.orkestrator.utils.TestContext
import org.junit.jupiter.api.Test
import java.util.UUID

class BehovApiTest {
    private val synkronBehovløser = mockk<SynkronBehovløser>()
    private val ident = "12345678910"
    private val søknadId = UUID.randomUUID()

    private val testModul: Application.() -> Unit = {
        install(Authentication) {
            jwt("azureAd-maskin") { azureAdMaskin() }
        }
        behovApi(synkronBehovløser)
    }

    @Test
    fun `svarer med verdien fra SynkronBehovløser`() =
        withMockAuthServerAndTestApplication(testModul) {
            every { synkronBehovløser.løs(Behov.Permittert, ident, søknadId) } returns true

            val response = postBehov("Permittert")

            response.status shouldBe HttpStatusCode.OK
            response.body<BehovResponseDTO>().verdi shouldBe true
        }

    @Test
    fun `søknad som ikke finnes gir 404`() =
        withMockAuthServerAndTestApplication(testModul) {
            every { synkronBehovløser.løs(any(), any(), any()) } throws SøknadFinnesIkke(søknadId)

            postBehov("Permittert").status shouldBe HttpStatusCode.NotFound
        }

    @Test
    fun `behov som ikke støttes synkront gir 400`() =
        withMockAuthServerAndTestApplication(testModul) {
            every { synkronBehovløser.løs(any(), any(), any()) } throws IkkeStøttetBehov(Behov.Ordinær)

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

    private suspend fun TestContext.postBehov(behovNavn: String) =
        client.post("/behov/$behovNavn") {
            bearerAuth(testAzureADMaskinToken())
            contentType(ContentType.Application.Json)
            setBody(BehovRequestDTO(ident, søknadId))
        }
}
