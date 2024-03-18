package no.nav.dagpenger.soknad.orkestrator.opplysning

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.soknad.orkestrator.db.Postgres.dataSource
import no.nav.dagpenger.soknad.orkestrator.db.Postgres.withMigratedDb
import no.nav.dagpenger.soknad.orkestrator.opplysning.db.OpplysningRepositoryPostgres
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

        val opplysning =
            Opplysning(
                beskrivendeId = beskrivendeId,
                svar = "2021-01-01",
                ident = ident,
                søknadsId = søknadsId,
            )

        withMigratedDb { repository.lagre(opplysning) }

        opplysningService.hentOpplysning(
            beskrivendeId = beskrivendeId,
            ident = ident,
            søknadsId = søknadsId.toString(),
        ) shouldBe opplysning
    }

    @Test
    fun `vi henter ikke opplysning dersom ett av kriteriene ikke stemmer`() {
        val beskrivendeId = "dagpenger-søknadsdato"
        val ident = "12345678910"
        val søknadsId = UUID.randomUUID()
        val behandligsId = UUID.randomUUID()

        val opplysning =
            Opplysning(
                beskrivendeId = beskrivendeId,
                svar = "2021-01-01",
                ident = ident,
                søknadsId = søknadsId,
            )

        withMigratedDb { repository.lagre(opplysning) }

        shouldThrow<NoSuchElementException> {
            opplysningService.hentOpplysning(
                beskrivendeId = beskrivendeId,
                ident = ident,
                søknadsId = UUID.randomUUID().toString(),
            )
        }
    }

    @Test
    fun `vi sender ut melding om løsning for opplysningsbehov i riktig format på rapiden`() {
        // TODO: Oppddater denne testen
//        val opplysning =
//            Opplysning(
//                ident = "12345678910",
//                søknadsId = UUID.randomUUID(),
//                beskrivendeId = "dagpenger-søknadsdato",
//                svar = listOf("2021-01-01"),
//            )
//
//        opplysningService.publiserMeldingOmOpplysningBehovLøsning(opplysning)
//
//        with(testRapid.inspektør) {
//            size shouldBe 1
//
//            field(0, "ident").asText() shouldBe "12345678910"
//            field(0, "søknad_id").asText() shouldBe opplysning.søknadsId.toString()
//            field(0, "@behov").toList().first().asText() shouldBe "urn:opplysning:dagpenger-søknadsdato"
//            field(0, "@løsning").get("urn:opplysning:dagpenger-søknadsdato").let {
//                it.get("status").asText() shouldBe "hypotese"
//                it.get("verdi").asText() shouldBe "2021-01-01"
//            }
//        }
    }
}
