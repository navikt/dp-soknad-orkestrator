package no.nav.dagpenger.soknad.orkestrator.opplysning

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson3.jackson
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.testing.testApplication
import no.nav.dagpenger.soknad.orkestrator.api.models.LandDTO
import no.nav.dagpenger.soknad.orkestrator.config.configure
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import tools.jackson.module.kotlin.readValue
import kotlin.test.Test

class LandApiTest {
    @Test
    fun `Land svarer med 200 OK og en liste med Land`() {
        testApplication {
            application {
                install(ContentNegotiation) {
                    jackson { configure() }
                }
                landApi()
            }

            val respons = client.get("/land")

            respons.status shouldBe HttpStatusCode.OK
            shouldNotThrow<IllegalArgumentException> {
                objectMapper.readValue<List<LandDTO>>(respons.bodyAsText())
            }
        }
    }
}
