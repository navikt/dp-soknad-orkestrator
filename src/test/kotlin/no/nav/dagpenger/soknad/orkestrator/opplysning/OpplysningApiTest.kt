package no.nav.dagpenger.soknad.orkestrator.opplysning

import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import no.nav.dagpenger.soknad.orkestrator.utils.TestApplication.testAzureADToken
import no.nav.dagpenger.soknad.orkestrator.utils.TestApplication.withMockAuthServerAndTestApplication
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test

class OpplysningApiTest {
    @BeforeEach
    fun setup() {
        System.setProperty("Grupper.saksbehandler", "saksbehandler")
    }

    @AfterEach
    fun cleanUp() {
        System.clearProperty("Grupper.saksbehandler")
    }

    @Test
    fun `Uautentiserte kall returnerer 401`() {
        withMockAuthServerAndTestApplication(moduleFunction = { opplysningApi() }) {
            client.get("/opplysning").status shouldBe HttpStatusCode.Unauthorized
        }
    }

    @Test
    fun `Kall med saksbehandlerADgruppe returnerer 200 OK`() {
        withMockAuthServerAndTestApplication(moduleFunction = { opplysningApi() }) {
            client.get("/opplysning") {
                header(HttpHeaders.Authorization, "Bearer $testAzureADToken")
            }.let { response ->
                response.status shouldBe HttpStatusCode.OK
            }
        }
    }
}
