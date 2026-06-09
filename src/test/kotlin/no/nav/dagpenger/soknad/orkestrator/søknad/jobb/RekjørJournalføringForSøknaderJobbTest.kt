package no.nav.dagpenger.soknad.orkestrator.søknad.jobb

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.soknad.orkestrator.søknad.Søknad
import no.nav.dagpenger.soknad.orkestrator.søknad.Tilstand
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class RekjørJournalføringForSøknaderJobbTest {
    private val testRapid = TestRapid()
    private val søknadRepository = mockk<SøknadRepository>(relaxed = true)

    private val søknadId1 = UUID.randomUUID()
    private val søknadId2 = UUID.randomUUID()
    private val søknadId3 = UUID.randomUUID()
    private val ident1 = "12345678901"
    private val ident2 = "12345678902"
    private val ident3 = "12345678903"

    @BeforeEach
    fun setup() {
        testRapid.reset()
    }

    @Test
    fun `kjørJobb publiserer søknad_klar_til_journalføring melding for hver søknad`() {
        val søknadIder = listOf(søknadId1, søknadId2)

        every { søknadRepository.hent(søknadId1) } returns Søknad(søknadId1, ident1, Tilstand.INNSENDT)
        every { søknadRepository.hent(søknadId2) } returns Søknad(søknadId2, ident2, Tilstand.INNSENDT)

        RekjørJournalføringForSøknaderJobb.kjørJobb(testRapid, søknadRepository, søknadIder)

        testRapid.inspektør.size shouldBe 2

        testRapid.inspektør.message(0)["@event_name"].asString() shouldBe "søknad_klar_til_journalføring"
        testRapid.inspektør.message(0)["søknadId"].asString() shouldBe søknadId1.toString()
        testRapid.inspektør.message(0)["ident"].asString() shouldBe ident1

        testRapid.inspektør.message(1)["@event_name"].asString() shouldBe "søknad_klar_til_journalføring"
        testRapid.inspektør.message(1)["søknadId"].asString() shouldBe søknadId2.toString()
        testRapid.inspektør.message(1)["ident"].asString() shouldBe ident2
    }

    @Test
    fun `kjørJobb hopper over søknader som ikke finnes i databasen`() {
        val søknadIder = listOf(søknadId1, søknadId2)

        every { søknadRepository.hent(søknadId1) } returns null
        every { søknadRepository.hent(søknadId2) } returns Søknad(søknadId2, ident2, Tilstand.INNSENDT)

        RekjørJournalføringForSøknaderJobb.kjørJobb(testRapid, søknadRepository, søknadIder)

        testRapid.inspektør.size shouldBe 1
        testRapid.inspektør.message(0)["søknadId"].asString() shouldBe søknadId2.toString()
    }

    @Test
    fun `kjørJobb hopper over søknader som allerede er journalført`() {
        val søknadIder = listOf(søknadId1, søknadId2, søknadId3)

        every { søknadRepository.hent(søknadId1) } returns Søknad(søknadId1, ident1, Tilstand.JOURNALFØRT)
        every { søknadRepository.hent(søknadId2) } returns Søknad(søknadId2, ident2, Tilstand.INNSENDT)
        every { søknadRepository.hent(søknadId3) } returns Søknad(søknadId3, ident3, Tilstand.JOURNALFØRT)

        RekjørJournalføringForSøknaderJobb.kjørJobb(testRapid, søknadRepository, søknadIder)

        testRapid.inspektør.size shouldBe 1
        testRapid.inspektør.message(0)["søknadId"].asString() shouldBe søknadId2.toString()
    }

    @Test
    fun `kjørJobb sender ingen meldinger når listen er tom`() {
        val søknadIder = emptyList<UUID>()

        RekjørJournalføringForSøknaderJobb.kjørJobb(testRapid, søknadRepository, søknadIder)

        testRapid.inspektør.size shouldBe 0
        verify(exactly = 0) { søknadRepository.hent(any()) }
    }

    @Test
    fun `kjørJobb fortsetter med neste søknad selv om en feiler`() {
        val søknadIder = listOf(søknadId1, søknadId2, søknadId3)

        every { søknadRepository.hent(søknadId1) } returns Søknad(søknadId1, ident1, Tilstand.INNSENDT)
        every { søknadRepository.hent(søknadId2) } throws RuntimeException("Database feil")
        every { søknadRepository.hent(søknadId3) } returns Søknad(søknadId3, ident3, Tilstand.INNSENDT)

        RekjørJournalføringForSøknaderJobb.kjørJobb(testRapid, søknadRepository, søknadIder)

        testRapid.inspektør.size shouldBe 2
        testRapid.inspektør.message(0)["søknadId"].asString() shouldBe søknadId1.toString()
        testRapid.inspektør.message(1)["søknadId"].asString() shouldBe søknadId3.toString()
    }

    @Test
    fun `kjørJobb henter søknad fra repository for hver søknadId`() {
        val søknadIder = listOf(søknadId1, søknadId2)

        every { søknadRepository.hent(søknadId1) } returns Søknad(søknadId1, ident1, Tilstand.INNSENDT)
        every { søknadRepository.hent(søknadId2) } returns Søknad(søknadId2, ident2, Tilstand.INNSENDT)

        RekjørJournalføringForSøknaderJobb.kjørJobb(testRapid, søknadRepository, søknadIder)

        verify(exactly = 1) { søknadRepository.hent(søknadId1) }
        verify(exactly = 1) { søknadRepository.hent(søknadId2) }
    }
}
