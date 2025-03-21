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
import no.nav.dagpenger.soknad.orkestrator.api.models.BarnResponseDTO
import no.nav.dagpenger.soknad.orkestrator.api.models.OppdatertBarnRequestDTO
import no.nav.dagpenger.soknad.orkestrator.behov.løsere.BarnetilleggBehovLøser.Companion.beskrivendeIdPdlBarn
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.QuizOpplysning
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.Barn
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.BarnSvar
import no.nav.dagpenger.soknad.orkestrator.utils.InMemoryQuizOpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.utils.TestApplication.testAzureADToken
import no.nav.dagpenger.soknad.orkestrator.utils.TestApplication.withMockAuthServerAndTestApplication
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test

class OpplysningApiTest {
    val opplysningRepository = InMemoryQuizOpplysningRepository()
    val opplysningService = OpplysningService(opplysningRepository)
    val søknadId = UUID.randomUUID()
    val ident = "12345678910"

    @BeforeEach
    fun setup() {
        System.setProperty("Grupper.saksbehandler", "saksbehandler")
    }

    @AfterEach
    fun cleanUp() {
        System.clearProperty("Grupper.saksbehandler")
    }

    @Test
    fun `Uautentiserte kall returnerer 401 Unauthorized`() {
        withMockAuthServerAndTestApplication(moduleFunction = { opplysningApi(opplysningService) }) {
            client.get("/opplysninger/${UUID.randomUUID()}/barn").status shouldBe HttpStatusCode.Unauthorized
        }
    }

    @Test
    fun `Kall med saksbehandlerADgruppe returnerer 200 OK`() {
        withMockAuthServerAndTestApplication(moduleFunction = { opplysningApi(opplysningService) }) {
            client.get("/opplysninger/${UUID.randomUUID()}/barn") {
                header(HttpHeaders.Authorization, "Bearer $testAzureADToken")
            }.status shouldBe HttpStatusCode.OK
        }
    }

    @Test
    fun `Hent opplysning med ugyldig søknadId returnerer 400 Bad Request`() {
        withMockAuthServerAndTestApplication(moduleFunction = { opplysningApi(opplysningService) }) {
            client.get("/opplysninger/ugyldigSøknadId/barn") {
                header(HttpHeaders.Authorization, "Bearer $testAzureADToken")
                header(HttpHeaders.ContentType, "application/json")
            }.status shouldBe HttpStatusCode.BadRequest
        }
    }

    @Test
    fun `Hent opplysning returnerer 200 OK`() {
        opplysningRepository.lagre(
            QuizOpplysning(
                beskrivendeId = beskrivendeIdPdlBarn,
                type = Barn,
                svar =
                    listOf(
                        BarnSvar(
                            barnSvarId = UUID.randomUUID(),
                            fornavnOgMellomnavn = "Kari",
                            etternavn = "Nordmann",
                            fødselsdato = LocalDate.of(2020, 1, 1),
                            statsborgerskap = "NOR",
                            forsørgerBarnet = false,
                            fraRegister = false,
                            kvalifisererTilBarnetillegg = false,
                        ),
                    ),
                ident = ident,
                søknadId = søknadId,
            ),
        )

        withMockAuthServerAndTestApplication(moduleFunction = { opplysningApi(opplysningService) }) {
            client.get("/opplysninger/$søknadId/barn") {
                header(HttpHeaders.Authorization, "Bearer $testAzureADToken")
            }.let { response ->
                response.status shouldBe HttpStatusCode.OK
                val barn = objectMapper.readValue<List<BarnResponseDTO>>(response.bodyAsText())
                barn.size shouldBe 1
            }
        }
    }

    @Test
    fun `Oppdater opplysning med ugyldig søknadId returnerer 400 Bad Request`() {
        withMockAuthServerAndTestApplication(moduleFunction = { opplysningApi(opplysningService) }) {
            client.put("/opplysninger/ugyldigSøknadId/barn/oppdater") {
                header(HttpHeaders.Authorization, "Bearer $testAzureADToken")
                header(HttpHeaders.ContentType, "application/json")
                setBody(oppdatertBarnRequestDTO)
            }.let { response ->
                response.status shouldBe HttpStatusCode.BadRequest
            }
        }
    }

    @Test
    fun `Oppdater opplysning som ikke finnes svarer med 404 Not Found`() {
        val opplysning =
            QuizOpplysning(
                beskrivendeId = beskrivendeIdPdlBarn,
                type = Barn,
                svar =
                    listOf(
                        BarnSvar(
                            barnSvarId = UUID.randomUUID(),
                            fornavnOgMellomnavn = "Kari",
                            etternavn = "Nordmann",
                            fødselsdato = LocalDate.of(2020, 1, 1),
                            statsborgerskap = "NOR",
                            forsørgerBarnet = false,
                            fraRegister = false,
                            kvalifisererTilBarnetillegg = false,
                        ),
                    ),
                ident = ident,
                søknadId = søknadId,
            )

        opplysningRepository.lagre(opplysning)

        withMockAuthServerAndTestApplication(moduleFunction = { opplysningApi(opplysningService) }) {
            client.put("/opplysninger/$søknadId/barn/oppdater") {
                header(HttpHeaders.Authorization, "Bearer $testAzureADToken")
                header(HttpHeaders.ContentType, "application/json")
                setBody(
                    OppdatertBarnRequestDTO(
                        barnId = opplysning.svar.first().barnSvarId,
                        fornavnOgMellomnavn = opplysning.svar.first().fornavnOgMellomnavn,
                        etternavn = opplysning.svar.first().etternavn,
                        fodselsdato = opplysning.svar.first().fødselsdato,
                        oppholdssted = opplysning.svar.first().statsborgerskap,
                        forsorgerBarnet = true,
                        kvalifisererTilBarnetillegg = false,
                        begrunnelse = "Begrunnelse",
                    ),
                )
            }.let { response ->
                response.status shouldBe HttpStatusCode.OK
            }
        }
    }

    @Test
    fun `Oppdater opplysning returnerer 400 Bad Request hvis fom og tom ikke er satt når kvalifisererTilBarnetillegg er true`() {
        val opplysning =
            QuizOpplysning(
                beskrivendeId = beskrivendeIdPdlBarn,
                type = Barn,
                svar =
                    listOf(
                        BarnSvar(
                            barnSvarId = UUID.randomUUID(),
                            fornavnOgMellomnavn = "Kari",
                            etternavn = "Nordmann",
                            fødselsdato = LocalDate.of(2020, 1, 1),
                            statsborgerskap = "NOR",
                            forsørgerBarnet = false,
                            fraRegister = false,
                            kvalifisererTilBarnetillegg = false,
                        ),
                    ),
                ident = ident,
                søknadId = søknadId,
            )

        opplysningRepository.lagre(opplysning)

        withMockAuthServerAndTestApplication(moduleFunction = { opplysningApi(opplysningService) }) {
            client.put("/opplysninger/$søknadId/barn/oppdater") {
                header(HttpHeaders.Authorization, "Bearer $testAzureADToken")
                header(HttpHeaders.ContentType, "application/json")
                setBody(
                    OppdatertBarnRequestDTO(
                        barnId = opplysning.svar.first().barnSvarId,
                        fornavnOgMellomnavn = opplysning.svar.first().fornavnOgMellomnavn,
                        etternavn = opplysning.svar.first().etternavn,
                        fodselsdato = opplysning.svar.first().fødselsdato,
                        oppholdssted = opplysning.svar.first().statsborgerskap,
                        forsorgerBarnet = opplysning.svar.first().forsørgerBarnet,
                        kvalifisererTilBarnetillegg = true,
                        begrunnelse = "Begrunnelse",
                    ),
                )
            }.let { response ->
                response.status shouldBe HttpStatusCode.BadRequest
            }
        }
    }

    @Test
    fun `Oppdater opplysning uten at gitt opplysning inneholder endringer returnerer 304 Not Modified`() {
        val opplysning =
            QuizOpplysning(
                beskrivendeId = beskrivendeIdPdlBarn,
                type = Barn,
                svar =
                    listOf(
                        BarnSvar(
                            barnSvarId = UUID.randomUUID(),
                            fornavnOgMellomnavn = "Kari",
                            etternavn = "Nordmann",
                            fødselsdato = LocalDate.of(2020, 1, 1),
                            statsborgerskap = "NOR",
                            forsørgerBarnet = false,
                            fraRegister = false,
                            kvalifisererTilBarnetillegg = false,
                        ),
                    ),
                ident = ident,
                søknadId = søknadId,
            )

        opplysningRepository.lagre(opplysning)

        withMockAuthServerAndTestApplication(moduleFunction = { opplysningApi(opplysningService) }) {
            client.put("/opplysninger/$søknadId/barn/oppdater") {
                header(HttpHeaders.Authorization, "Bearer $testAzureADToken")
                header(HttpHeaders.ContentType, "application/json")
                setBody(
                    OppdatertBarnRequestDTO(
                        barnId = opplysning.svar.first().barnSvarId,
                        fornavnOgMellomnavn = opplysning.svar.first().fornavnOgMellomnavn,
                        etternavn = opplysning.svar.first().etternavn,
                        fodselsdato = opplysning.svar.first().fødselsdato,
                        oppholdssted = opplysning.svar.first().statsborgerskap,
                        forsorgerBarnet = opplysning.svar.first().forsørgerBarnet,
                        kvalifisererTilBarnetillegg = false,
                        begrunnelse = "Begrunnelse",
                    ),
                )
            }.let { response ->
                response.status shouldBe HttpStatusCode.NotModified
            }
        }
    }

    @Test
    fun `Oppdater opplysning svarer med 200 OK`() {
        val opplysning =
            QuizOpplysning(
                beskrivendeId = beskrivendeIdPdlBarn,
                type = Barn,
                svar =
                    listOf(
                        BarnSvar(
                            barnSvarId = UUID.randomUUID(),
                            fornavnOgMellomnavn = "Kari",
                            etternavn = "Nordmann",
                            fødselsdato = LocalDate.of(2020, 1, 1),
                            statsborgerskap = "NOR",
                            forsørgerBarnet = false,
                            fraRegister = false,
                            kvalifisererTilBarnetillegg = false,
                        ),
                    ),
                ident = ident,
                søknadId = søknadId,
            )

        opplysningRepository.lagre(opplysning)

        withMockAuthServerAndTestApplication(moduleFunction = { opplysningApi(opplysningService) }) {
            client.put("/opplysninger/$søknadId/barn/oppdater") {
                header(HttpHeaders.Authorization, "Bearer $testAzureADToken")
                header(HttpHeaders.ContentType, "application/json")
                setBody(
                    OppdatertBarnRequestDTO(
                        barnId = opplysning.svar.first().barnSvarId,
                        fornavnOgMellomnavn = opplysning.svar.first().fornavnOgMellomnavn,
                        etternavn = opplysning.svar.first().etternavn,
                        fodselsdato = opplysning.svar.first().fødselsdato,
                        oppholdssted = opplysning.svar.first().statsborgerskap,
                        forsorgerBarnet = opplysning.svar.first().forsørgerBarnet,
                        kvalifisererTilBarnetillegg = true,
                        barnetilleggFom = LocalDate.of(2020, 1, 1),
                        barnetilleggTom = LocalDate.of(2038, 1, 1),
                        begrunnelse = "Begrunnelse",
                    ),
                )
            }.let { response ->
                response.status shouldBe HttpStatusCode.OK
            }
        }
    }
}

val oppdatertBarnRequestDTO =
    OppdatertBarnRequestDTO(
        barnId = UUID.randomUUID(),
        fornavnOgMellomnavn = "Ola",
        etternavn = "Nordmann",
        fodselsdato = LocalDate.of(2020, 1, 1),
        oppholdssted = "Norge",
        forsorgerBarnet = true,
        kvalifisererTilBarnetillegg = true,
        barnetilleggFom = LocalDate.of(2020, 1, 1),
        barnetilleggTom = LocalDate.of(2038, 1, 1),
        begrunnelse = "Begrunnelse",
    )
