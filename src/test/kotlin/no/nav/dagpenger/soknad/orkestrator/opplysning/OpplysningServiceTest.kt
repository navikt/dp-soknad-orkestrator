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
    private val opplysningService = OpplysningService(repository)

    @BeforeEach
    fun reset() {
        testRapid.reset()
    }

    @Test
    fun `vi henter opplysning ved riktige kriterium`() {
        val beskrivendeId = "dagpenger-søknadsdato"
        val ident = "12345678910"
        val søknadId = UUID.randomUUID()
        val behandligId = UUID.randomUUID()
        val forventetOpplysning = opplysningMed(beskrivendeId, ident, søknadId)

        withMigratedDb { repository.lagre(forventetOpplysning) }

        opplysningService.hentOpplysning(
            beskrivendeId = beskrivendeId,
            ident = ident,
            søknadId = søknadId.toString(),
            behandlingId = behandligId.toString(),
        ) shouldBe forventetOpplysning
    }

    @Test
    fun `vi henter ikke opplysning dersom ett av kriteriene ikke stemmer`() {
        val beskrivendeId = "dagpenger-søknadsdato"
        val ident = "12345678910"
        val søknadId = UUID.randomUUID()
        val behandligId = UUID.randomUUID()
        val forventetOpplysning = opplysningMed(beskrivendeId, ident, søknadId)

        withMigratedDb { repository.lagre(forventetOpplysning) }

        shouldThrow<NoSuchElementException> {
            opplysningService.hentOpplysning(
                beskrivendeId = beskrivendeId,
                ident = ident,
                søknadId = UUID.randomUUID().toString(),
                behandlingId = behandligId.toString(),
            )
        }
    }
}

fun opplysningMed(
    beskrivendeId: String,
    ident: String,
    søknadId: UUID = UUID.randomUUID(),
    svar: List<String> = listOf("svar"),
) = Opplysning(
    fødselsnummer = ident,
    søknadsId = søknadId,
    beskrivendeId = beskrivendeId,
    svar = svar,
)
