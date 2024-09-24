package no.nav.dagpenger.soknad.orkestrator.opplysning.db

import io.kotest.matchers.shouldBe
import io.mockk.mockk
import no.nav.dagpenger.soknad.orkestrator.db.Postgres.dataSource
import no.nav.dagpenger.soknad.orkestrator.db.Postgres.withMigratedDb
import no.nav.dagpenger.soknad.orkestrator.opplysning.BooleanSvar
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysning
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysningstype.BOOLEAN
import no.nav.dagpenger.soknad.orkestrator.søknad.Søknad
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import org.junit.jupiter.api.Disabled
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test

class OpplysningRepositoryTest {
    private lateinit var søknadRepository: SøknadRepository
    private lateinit var opplysningRepository: OpplysningRepository

    @BeforeTest
    fun setup() {
        withMigratedDb {
            søknadRepository =
                SøknadRepository(
                    dataSource = dataSource,
                    quizOpplysningRepository = mockk(),
                )
            opplysningRepository = OpplysningRepository(dataSource)
        }
    }

    // TODO: Denne testen feiler fordi vi ikke lagrer seksjoner i DB.
    @Disabled
    @Test
    fun `Kan lagre og hente opplysning`() {
        val opplysningId = UUID.randomUUID()
        val søknad = Søknad(UUID.randomUUID(), "1234567890")

        val opplysning =
            Opplysning(
                opplysningId = opplysningId,
                opplysningsbehovId = 1,
                type = BOOLEAN,
                svar = BooleanSvar(opplysningId, true),
            )

        søknadRepository.lagre(søknad)
        opplysningRepository.lagre(søknad.søknadId, opplysning)
        val hentetOpplysning = opplysningRepository.hent(opplysningId)

        hentetOpplysning?.opplysningId shouldBe opplysning.opplysningId
        hentetOpplysning?.opplysningsbehovId shouldBe opplysning.opplysningsbehovId
        hentetOpplysning?.type shouldBe opplysning.type
        // hentetOpplysning?.svar shouldBe opplysning.svar
    }
}
