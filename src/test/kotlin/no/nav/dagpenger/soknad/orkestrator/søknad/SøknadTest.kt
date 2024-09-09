package no.nav.dagpenger.soknad.orkestrator.søknad

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.soknad.orkestrator.api.models.SporsmaalgruppeNavnDTO
import no.nav.dagpenger.soknad.orkestrator.api.models.SporsmalgruppeDTO
import no.nav.dagpenger.soknad.orkestrator.config.apiKonfigurasjon
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import no.nav.dagpenger.soknad.orkestrator.spørsmål.LandSvar
import no.nav.dagpenger.soknad.orkestrator.spørsmål.grupper.Bostedsland
import no.nav.dagpenger.soknad.orkestrator.søknad.db.InMemorySøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.utils.TestApplication
import no.nav.dagpenger.soknad.orkestrator.utils.TestApplication.autentisert
import no.nav.helse.rapids_rivers.RapidsConnection
import java.util.UUID
import kotlin.test.Test

class SøknadTest {
    val søknadEndepunkt = "/soknad"

    val rapid = mockk<RapidsConnection>(relaxed = true)
    val søknadRepository = mockk<SøknadRepository>(relaxed = true)
    val inMemorySøknadRepository = InMemorySøknadRepository()
    val søknadService =
        SøknadService(
            rapid = rapid,
            søknadRepository = søknadRepository,
            inMemorySøknadRepository = inMemorySøknadRepository,
        )

    @Test
    fun `Det er mulig å starte søknad og få neste spørsmål til det er tomt`() {
        withSøknadApi {
            autentisert(
                endepunkt = "$søknadEndepunkt/start",
                httpMethod = HttpMethod.Post,
            ).let { respons ->
                respons.status shouldBe HttpStatusCode.Created
                val søknadId = objectMapper.readValue(respons.bodyAsText(), UUID::class.java)
                verify { søknadRepository.lagre(any<Søknad>()) }
                inMemorySøknadRepository.hentAlle(søknadId).size shouldBe 1
            }
        }

        val gjeldendeSøknadId = inMemorySøknadRepository.hentAlleKeys().first()
        withSøknadApi {
            autentisert(
                endepunkt = "$søknadEndepunkt/$gjeldendeSøknadId/neste",
                httpMethod = HttpMethod.Get,
            ).let { respons ->
                respons.status shouldBe HttpStatusCode.OK
                val spørsmalgruppe = objectMapper.readValue(respons.bodyAsText(), SporsmalgruppeDTO::class.java)
                spørsmalgruppe.navn shouldBe SporsmaalgruppeNavnDTO.BOSTEDSLAND
                spørsmalgruppe.nesteSpørsmål shouldNotBe null
                spørsmalgruppe.nesteSpørsmål!!.tekstnøkkel shouldBe Bostedsland.hvilketLandBorDuI.tekstnøkkel
                spørsmalgruppe.erFullført shouldBe false
            }
        }

        val gjeldendeSpørsmålInfo = inMemorySøknadRepository.hentAlle(gjeldendeSøknadId).first()
        withSøknadApi {
            autentisert(
                endepunkt = "$søknadEndepunkt/$gjeldendeSøknadId/svar",
                httpMethod = HttpMethod.Post,
                body =
                    objectMapper.writeValueAsString(
                        LandSvar(
                            spørsmålId = gjeldendeSpørsmålInfo.spørsmålId,
                            verdi = "NOR",
                        ),
                    ),
            ).let { respons ->
                respons.status shouldBe HttpStatusCode.OK
            }
        }

        withSøknadApi {
            autentisert(
                endepunkt = "$søknadEndepunkt/$gjeldendeSøknadId/neste",
                httpMethod = HttpMethod.Get,
            ).let { respons ->
                respons.status shouldBe HttpStatusCode.OK
                val spørsmalgruppe = objectMapper.readValue(respons.bodyAsText(), SporsmalgruppeDTO::class.java)
                spørsmalgruppe.navn shouldBe SporsmaalgruppeNavnDTO.BOSTEDSLAND
                spørsmalgruppe.nesteSpørsmål shouldBe null
                spørsmalgruppe.besvarteSpørsmål.size shouldBe 1
                spørsmalgruppe.besvarteSpørsmål.first().svar shouldBe "NOR"
                spørsmalgruppe.erFullført shouldBe true
            }
        }
    }

    private fun withSøknadApi(test: suspend ApplicationTestBuilder.() -> Unit) {
        TestApplication.withMockAuthServerAndTestApplication(
            moduleFunction = {
                apiKonfigurasjon()
                søknadApi(søknadService)
            },
            test = test,
        )
    }
}
