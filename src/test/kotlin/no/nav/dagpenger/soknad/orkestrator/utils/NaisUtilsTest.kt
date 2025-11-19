package no.nav.dagpenger.soknad.orkestrator.utils

import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import io.mockk.every
import io.mockk.mockkStatic
import kotlin.test.Test

class NaisUtilsTest {
    @Test
    fun `isLeader returnerer true hvis ELECTOR_GET_URL og HOSTNAME er satt, og HOSTNAME er leader`() {
        mockkStatic(::getSystemEnv) {
            every { getSystemEnv("HOSTNAME") } returns "646b7f7b5-hc44t"
            every { getSystemEnv("ELECTOR_GET_URL") } returns "https://example.com/"

            NaisUtils().isLeader(configureHttpClient(lagMockEngine())) shouldBe true
        }
    }

    @Test
    fun `isLeader returnerer false hvis ELECTOR_GET_URL og HOSTNAME er satt, men HOSTNAME ikke er leader`() {
        mockkStatic(::getSystemEnv) {
            every { getSystemEnv("HOSTNAME") } returns "et-annet-hostname"
            every { getSystemEnv("ELECTOR_GET_URL") } returns "https://example.com/"

            NaisUtils().isLeader(configureHttpClient(lagMockEngine())) shouldBe false
        }
    }

    @Test
    fun `isLeader returnerer false hvis ELECTOR_GET_URL er blank og HOSTNAME er satt`() {
        mockkStatic(::getSystemEnv) {
            every { getSystemEnv("HOSTNAME") } returns "646b7f7b5-hc44t"
            every { getSystemEnv("ELECTOR_GET_URL") } returns "   "

            NaisUtils().isLeader(configureHttpClient(lagMockEngine())) shouldBe false
        }
    }

    @Test
    fun `isLeader returnerer false hvis ELECTOR_GET_URL er null og HOSTNAME er satt`() {
        mockkStatic(::getSystemEnv) {
            every { getSystemEnv("HOSTNAME") } returns "646b7f7b5-hc44t"
            every { getSystemEnv("ELECTOR_GET_URL") } returns null

            NaisUtils().isLeader(configureHttpClient(lagMockEngine())) shouldBe false
        }
    }

    @Test
    fun `isLeader returnerer false hvis ELECTOR_GET_URL er satt og HOSTNAME er blank`() {
        mockkStatic(::getSystemEnv) {
            every { getSystemEnv("HOSTNAME") } returns "   "
            every { getSystemEnv("ELECTOR_GET_URL") } returns "https://example.com/"

            NaisUtils().isLeader(configureHttpClient(lagMockEngine())) shouldBe false
        }
    }

    @Test
    fun `isLeader returnerer false hvis ELECTOR_GET_URL er satt og HOSTNAME er null`() {
        mockkStatic(::getSystemEnv) {
            every { getSystemEnv("HOSTNAME") } returns null
            every { getSystemEnv("ELECTOR_GET_URL") } returns "https://example.com/"

            NaisUtils().isLeader(configureHttpClient(lagMockEngine())) shouldBe false
        }
    }

    @Test
    fun `isLeader returnerer false hvis ELECTOR_GET_URL er null og HOSTNAME er null`() {
        mockkStatic(::getSystemEnv) {
            every { getSystemEnv("HOSTNAME") } returns null
            every { getSystemEnv("ELECTOR_GET_URL") } returns null

            NaisUtils().isLeader(configureHttpClient(lagMockEngine())) shouldBe false
        }
    }

    @Test
    fun `isLeader returnerer false hvis ELECTOR_GET_URL er satt og HOSTNAME er satt, men kall til ELECTOR_GET_URL feiler`() {
        mockkStatic(::getSystemEnv) {
            every { getSystemEnv("HOSTNAME") } returns "646b7f7b5-hc44t"
            every { getSystemEnv("ELECTOR_GET_URL") } returns "https://example.com/"

            NaisUtils().isLeader(configureHttpClient(lagMockEngine("ugyldig json"))) shouldBe false
        }
    }

    private fun lagMockEngine(response: String = """{"name": "646b7f7b5-hc44t", "last_update": "2025-11-15"}"""): MockEngine =
        MockEngine {
            respond(
                content = ByteReadChannel(response),
                status = HttpStatusCode.OK,
                headers = headersOf(ContentType, "application/json"),
            )
        }
}
