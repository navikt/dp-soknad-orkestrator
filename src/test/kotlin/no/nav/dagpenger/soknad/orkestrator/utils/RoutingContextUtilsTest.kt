package no.nav.dagpenger.soknad.orkestrator.utils

import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import java.util.UUID.randomUUID
import kotlin.test.Test

class RoutingContextUtilsTest {
    @Test
    fun `validerOgFormaterSøknadIdParam returnerer søkadId hvis søknadId er en gyldig UUID`() {
        val routingContext = mockk<RoutingContext>(relaxed = true)
        val søknadId = randomUUID()
        coEvery {
            routingContext.call.parameters["søknadId"]
        } returns søknadId.toString()

        runBlocking {
            routingContext.validerOgFormaterSøknadIdParam() shouldBe søknadId
        }
    }

    @Test
    fun `validerOgFormaterSøknadIdParam returnerer null hvis søknadId er null`() {
        val routingContext = mockk<RoutingContext>(relaxed = true)
        coEvery {
            routingContext.call.parameters["søknadId"]
        } returns null

        runBlocking {
            routingContext.validerOgFormaterSøknadIdParam() shouldBe null
        }
    }

    @Test
    fun `validerOgFormaterSøknadIdParam returnerer null og sender 400 Bad Request hvis søknadId ikke er null og ikke er en gyldig UUID`() {
        val routingContext = mockk<RoutingContext>(relaxed = true)
        coEvery {
            routingContext.call.parameters["søknadId"]
        } returns "ikke-en-uuid"

        runBlocking {
            routingContext.validerOgFormaterSøknadIdParam() shouldBe null
            coVerify { routingContext.call.respond(BadRequest, any<String>()) }
        }
    }

    @Test
    fun `validerSeksjonIdParam returnerer null og sender 400 Bad Request hvis seksjonId ikke er null`() {
        val routingContext = mockk<RoutingContext>(relaxed = true)
        coEvery {
            routingContext.call.parameters["seksjonId"]
        } returns "seksjon-id"

        runBlocking {
            routingContext.validerSeksjonIdParam() shouldBe "seksjon-id"
        }
    }

    @Test
    fun `validerSeksjonIdParam returnerer null og sender 400 Bad Request hvis seksjonId er null`() {
        val routingContext = mockk<RoutingContext>(relaxed = true)
        coEvery {
            routingContext.call.parameters["seksjonId"]
        } returns null

        runBlocking {
            routingContext.validerSeksjonIdParam() shouldBe null
            coVerify { routingContext.call.respond(BadRequest, any<String>()) }
        }
    }
}
