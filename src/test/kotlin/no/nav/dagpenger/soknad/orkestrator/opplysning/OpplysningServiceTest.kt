package no.nav.dagpenger.soknad.orkestrator.opplysning

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.soknad.orkestrator.db.Postgres.dataSource
import no.nav.dagpenger.soknad.orkestrator.db.Postgres.withMigratedDb
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.NoSuchElementException
import java.util.UUID

class OpplysningServiceTest {
    private val testRapid = TestRapid()
    private val repository = OpplysningRepositoryPostgres(dataSource)
    private val opplysningService = OpplysningService(testRapid, repository)

    @BeforeEach
    fun reset() {
        testRapid.reset()
    }

    @Test
    fun `vi henter opplysning ved riktige kriterium`() {
        val beskrivendeId = "dagpenger-søknadsdato"
        val ident = "12345678910"
        val søknadsId = UUID.randomUUID()
        val behandligsId = UUID.randomUUID()
        val forventetOpplysning = opplysningMed(beskrivendeId, ident, søknadsId, behandligsId)

        withMigratedDb { repository.lagre(forventetOpplysning) }

        opplysningService.hentOpplysning(
            beskrivendeId = beskrivendeId,
            ident = ident,
            søknadsId = søknadsId.toString(),
            behandlingsId = behandligsId.toString(),
        ) shouldBe forventetOpplysning
    }

    @Test
    fun `vi henter ikke opplysning dersom ett av kriteriene ikke stemmer`() {
        val beskrivendeId = "dagpenger-søknadsdato"
        val ident = "12345678910"
        val søknadsId = UUID.randomUUID()
        val behandligsId = UUID.randomUUID()
        val forventetOpplysning = opplysningMed(beskrivendeId, ident, søknadsId)

        withMigratedDb { repository.lagre(forventetOpplysning) }

        shouldThrow<NoSuchElementException> {
            opplysningService.hentOpplysning(
                beskrivendeId = beskrivendeId,
                ident = ident,
                søknadsId = UUID.randomUUID().toString(),
                behandlingsId = behandligsId.toString(),
            )
        }
    }

    @Test
    fun `vi kan sende ut melding om løsning til opplysningsbehov på rapiden`() {
        val opplysning =
            Opplysning(
                ident = "12345678910",
                søknadsId = UUID.randomUUID(),
                beskrivendeId = "dagpenger-søknadsdato",
                svar = listOf("2021-01-01"),
            )

        opplysningService.publiserMeldingOmOpplysningBehovLøsning(opplysning)

        with(testRapid.inspektør) {
            size shouldBe 1

            field(0, "ident").asText() shouldBe "12345678910"
            field(0, "søknad_id").asText() shouldBe opplysning.søknadsId.toString()
            field(0, "behandling_id").asText() shouldBe opplysning.behandlingsId.toString()

            field(0, "@behov").toList().first().asText() shouldBe "urn:opplysning:dagpenger-søknadsdato"
            field(0, "@løsning").get("urn:opplysning:dagpenger-søknadsdato:hypotese").asText() shouldBe "2021-01-01"
        }
    }
}

fun opplysningMed(
    beskrivendeId: String,
    ident: String,
    søknadsId: UUID = UUID.randomUUID(),
    behandlingsId: UUID = UUID.randomUUID(),
    svar: List<String> = listOf("svar"),
) = Opplysning(
    ident = ident,
    søknadsId = søknadsId,
    behandlingsId = behandlingsId,
    beskrivendeId = beskrivendeId,
    svar = svar,
)
