package no.nav.dagpenger.soknad.orkestrator.opplysning

import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import no.nav.dagpenger.soknad.orkestrator.utils.InMemoryQuizOpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.utils.TestApplication.testAzureADToken
import no.nav.dagpenger.soknad.orkestrator.utils.TestApplication.withMockAuthServerAndTestApplication
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test

class OpplysningApiTest {
    val opplysningService = OpplysningService(InMemoryQuizOpplysningRepository())

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
        withMockAuthServerAndTestApplication(moduleFunction = { opplysningApi(opplysningService) }) {
            client.get("/opplysninger").status shouldBe HttpStatusCode.Unauthorized
        }
    }

    @Test
    fun `Kall med saksbehandlerADgruppe returnerer 200 OK`() {
        withMockAuthServerAndTestApplication(moduleFunction = { opplysningApi(opplysningService) }) {
            client.get("/opplysninger") {
                header(HttpHeaders.Authorization, "Bearer $testAzureADToken")
            }.let { response ->
                response.status shouldBe HttpStatusCode.OK
            }
        }
    }

    @Test
    fun `Kan hente ut opplysning om barn`() {
        withMockAuthServerAndTestApplication(moduleFunction = { opplysningApi(opplysningService) }) {
            client.get("/opplysninger/barn") {
                header(HttpHeaders.Authorization, "Bearer $testAzureADToken")
            }.let { response ->
                response.status shouldBe HttpStatusCode.OK
                val barn = objectMapper.readValue<List<BarnDTO>>(response.bodyAsText())
                barn.size shouldBe 2
            }
        }
    }

    @Test
    fun `Kan redigere opplysning om barn`() {
        withMockAuthServerAndTestApplication(moduleFunction = { opplysningApi(opplysningService) }) {
            client.put("/opplysninger/barn") {
                header(HttpHeaders.Authorization, "Bearer $testAzureADToken")
                setBody(
                    BarnDTO(
                        barnSvarId = UUID.randomUUID(),
                        fornavnOgMellomnavn = "Ola",
                        etternavn = "Nordmann",
                        fødselsdato = LocalDate.of(2020, 1, 1),
                        oppholdssted = "Norge",
                        forsørgerBarnet = true,
                        fraRegister = true,
                        girBarnetillegg = true,
                        girBarnetilleggFom = LocalDate.of(2020, 1, 1),
                        girBarnetilleggTom = LocalDate.of(2038, 1, 1),
                        begrunnelse = "Begrunnelse",
                        endretAv = "saksbehandler",
                    ),
                )
                header(HttpHeaders.ContentType, "application/json")
            }.let { response ->
                response.status shouldBe HttpStatusCode.OK
            }
        }
    }
}
