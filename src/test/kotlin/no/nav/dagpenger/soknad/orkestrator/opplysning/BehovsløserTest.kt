package no.nav.dagpenger.soknad.orkestrator.opplysning

import io.kotest.matchers.shouldBe
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import java.util.UUID
import kotlin.test.Test

class ØnskerDagpengerFraDatoBehovløserTest {
    val opplysningRepository = InMemoryOpplysningRepository()
    val testRapid = TestRapid()

    @Test
    fun `Behovløser publiserer løsning på behov`() {
        val ident = "12345678910"
        val søknadsId = UUID.randomUUID()
        val behandlingsId = UUID.randomUUID()
        val svar = "2021-01-01"

        val opplysning =
            Opplysning(
                beskrivendeId = "dagpenger-soknadsdato",
                ident = ident,
                søknadsId = søknadsId,
                behandlingsId = behandlingsId,
                svar = svar,
            )

        opplysningRepository.lagre(opplysning)
        val behovløser = ØnskerDagpengerFraDatoBehovløser(testRapid, opplysningRepository)
        behovløser.løs(ident, søknadsId, behandlingsId)

        testRapid.inspektør.message(0)["@løsning"]["ØnskerDagpengerFraDato"]["verdi"].asText() shouldBe svar
    }
}

class InMemoryOpplysningRepository : OpplysningRepository {
    private val opplysninger = mutableListOf<Opplysning>()

    override fun lagre(opplysning: Opplysning) {
        opplysninger.add(opplysning)
    }

    override fun hent(
        beskrivendeId: String,
        ident: String,
        søknadsId: UUID,
        behandlingsId: UUID,
    ): Opplysning {
        return opplysninger.find {
            it.beskrivendeId == beskrivendeId && it.ident == ident && it.søknadsId == søknadsId && it.behandlingsId == behandlingsId
        }
            ?: throw IllegalArgumentException("Fant ikke opplysning")
    }
}
