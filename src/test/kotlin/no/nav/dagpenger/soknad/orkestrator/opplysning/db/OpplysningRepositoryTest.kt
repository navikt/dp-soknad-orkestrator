package no.nav.dagpenger.soknad.orkestrator.opplysning.db

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import no.nav.dagpenger.soknad.orkestrator.db.Postgres.dataSource
import no.nav.dagpenger.soknad.orkestrator.db.Postgres.withMigratedDb
import no.nav.dagpenger.soknad.orkestrator.opplysning.BooleanSvar
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysning
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysningstype.BOOLEAN
import no.nav.dagpenger.soknad.orkestrator.søknad.Søknad
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
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

    @Test
    fun `Kan lagre og hente opplysning`() {
        val opplysningId = UUID.randomUUID()
        val søknad = Søknad(UUID.randomUUID(), "1234567890")
        val seksjonversjon = "SEKSJON_V1"

        val opplysning =
            Opplysning(
                opplysningId = opplysningId,
                seksjonversjon = seksjonversjon,
                opplysningsbehovId = 1,
                type = BOOLEAN,
                svar = BooleanSvar(opplysningId, true),
            )

        søknadRepository.lagre(søknad)
        opplysningRepository.opprettSeksjon(søknad.søknadId, seksjonversjon)
        opplysningRepository.lagre(søknad.søknadId, opplysning)
        val hentetOpplysning = opplysningRepository.hent(opplysningId)

        hentetOpplysning?.opplysningId shouldBe opplysning.opplysningId
        hentetOpplysning?.seksjonversjon shouldBe opplysning.seksjonversjon
        hentetOpplysning?.opplysningsbehovId shouldBe opplysning.opplysningsbehovId
        hentetOpplysning?.type shouldBe opplysning.type
        hentetOpplysning?.svar?.verdi shouldBe opplysning.svar?.verdi
    }

    @Test
    fun `Lagring av opplysning feiler hvis seksjon ikke er lagret`() {
        val opplysningId = UUID.randomUUID()
        val søknad = Søknad(UUID.randomUUID(), "1234567890")
        val seksjonversjon = "SEKSJON_V1"

        val opplysning =
            Opplysning(
                opplysningId = opplysningId,
                seksjonversjon = seksjonversjon,
                opplysningsbehovId = 1,
                type = BOOLEAN,
                svar = BooleanSvar(opplysningId, true),
            )

        søknadRepository.lagre(søknad)

        shouldThrow<IllegalStateException> {
            opplysningRepository.lagre(søknad.søknadId, opplysning)
        }.message shouldBe "Fant ikke seksjon med søknadId ${søknad.søknadId}"
    }

    @Test
    fun `Lagring av seksjon feiler hvis søknad ikke er lagret`() {
        val søknad = Søknad(UUID.randomUUID(), "1234567890")
        val seksjonversjon = "SEKSJON_V1"

        shouldThrow<IllegalStateException> {
            opplysningRepository.opprettSeksjon(søknad.søknadId, seksjonversjon)
        }.message shouldBe "Fant ikke søknad med id ${søknad.søknadId}"
    }
}
