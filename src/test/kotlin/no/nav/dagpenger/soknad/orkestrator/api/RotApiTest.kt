package no.nav.dagpenger.soknad.orkestrator.api

import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Test

class RotApiTest {
    @Test
    fun `rot svarer med 200 OK uten autentisering`() {
        testApplication {
            application { rotApi() }

            client.get("/").status shouldBe HttpStatusCode.OK
        }
    }
}
