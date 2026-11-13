package no.nav.dagpenger.soknad.orkestrator.opplysning

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import no.nav.dagpenger.soknad.orkestrator.db.Postgres.dataSource
import no.nav.dagpenger.soknad.orkestrator.db.Postgres.withMigratedDb
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.datatyper.BarnSvar
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.Søknad
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.utils.januar
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.OffsetDateTime
import java.util.UUID
import java.util.UUID.randomUUID
import kotlin.test.Test

class SaksbehandlerBarnRepositoryPostgresTest {
    private val ident = "12345678910"

    @Test
    fun `hentBarn returnerer null når søknaden ikke har noe snapshot`() {
        withMigratedDb {
            val repository = SaksbehandlerBarnRepositoryPostgres(dataSource)

            repository.hentBarn(opprettSøknad()) shouldBe null
        }
    }

    @Test
    fun `hentBarn returnerer tom liste når siste snapshot er tomt`() {
        withMigratedDb {
            val repository = SaksbehandlerBarnRepositoryPostgres(dataSource)
            val søknadId = opprettSøknad()
            lagreSnapshot(søknadId, listOf(barn("Første")), OffsetDateTime.now().minusMinutes(1))
            lagreSnapshot(søknadId, emptyList(), OffsetDateTime.now())

            repository.hentBarn(søknadId)!!.shouldBeEmpty()
        }
    }

    @Test
    fun `hentBarn returnerer snapshot med nyeste opprettet-tidspunkt`() {
        withMigratedDb {
            val repository = SaksbehandlerBarnRepositoryPostgres(dataSource)
            val søknadId = opprettSøknad()
            val nå = OffsetDateTime.now()
            lagreSnapshot(søknadId, listOf(barn("Nyest")), nå)
            lagreSnapshot(søknadId, listOf(barn("Eldst")), nå.minusMinutes(5))

            repository.hentBarn(søknadId)!!.map { it.fornavnOgMellomnavn } shouldBe listOf("Nyest")
        }
    }

    @Test
    fun `hentBarn returnerer sist lagrede snapshot når opprettet-tidspunktet er likt`() {
        withMigratedDb {
            val repository = SaksbehandlerBarnRepositoryPostgres(dataSource)
            val søknadId = opprettSøknad()
            val tidspunkt = OffsetDateTime.now()
            lagreSnapshot(søknadId, listOf(barn("Først lagret")), tidspunkt)
            lagreSnapshot(søknadId, listOf(barn("Sist lagret")), tidspunkt)

            repository.hentBarn(søknadId)!!.map { it.fornavnOgMellomnavn } shouldBe listOf("Sist lagret")
        }
    }

    private fun opprettSøknad(): UUID =
        randomUUID().also {
            SøknadRepository(dataSource, mockk<QuizOpplysningRepository>(relaxed = true)).opprett(Søknad(it, ident))
        }

    private fun lagreSnapshot(
        søknadId: UUID,
        barn: List<BarnSvar>,
        opprettet: OffsetDateTime,
    ) {
        transaction {
            SaksbehandlerBarnTabell.insert {
                it[SaksbehandlerBarnTabell.søknadId] = søknadId
                it[SaksbehandlerBarnTabell.barn] = objectMapper.writeValueAsString(barn)
                it[endretAv] = "Z991234"
                it[SaksbehandlerBarnTabell.opprettet] = opprettet
            }
        }
    }

    private fun barn(fornavn: String) =
        BarnSvar(
            barnSvarId = randomUUID(),
            fornavnOgMellomnavn = fornavn,
            etternavn = "Barn",
            fødselsdato = 1.januar(2010),
            statsborgerskap = "NOR",
            forsørgerBarnet = true,
            fraRegister = true,
            kvalifisererTilBarnetillegg = false,
        )
}
