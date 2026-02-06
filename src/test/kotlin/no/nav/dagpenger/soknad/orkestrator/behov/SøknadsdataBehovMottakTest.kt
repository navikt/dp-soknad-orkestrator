package no.nav.dagpenger.soknad.orkestrator.behov

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepositoryPostgres
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.seksjon.SeksjonRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class SøknadsdataBehovMottakTest {
    private val testRapid = TestRapid()
    val opplysningRepository = mockk<QuizOpplysningRepositoryPostgres>(relaxed = true)
    val seksjonRepository = mockk<SeksjonRepository>(relaxed = true)
    val søknadRepository = mockk<SøknadRepository>(relaxed = true)
    val ident = "12345678910"
    val søknadId = UUID.randomUUID()
    val now = LocalDate.now()

    init {
        SøknadsdataBehovMottak(
            rapidsConnection = testRapid,
            opplysningRepository = opplysningRepository,
            seksjonRepository = seksjonRepository,
            søknadRepository = søknadRepository,
        )
    }

    @BeforeEach
    fun setup() {
        testRapid.reset()
        clearMocks(opplysningRepository, seksjonRepository, søknadRepository)

        every {
            søknadRepository.hentSøknadIdFraJournalPostId(any(), any())
        } returns søknadId

        every {
            seksjonRepository.hentSeksjonsvarEllerKastException(
                ident,
                any(),
                "din-situasjon",
            )
        } returns objectMapper.readTree(dinSituasjonMedGjenopptakelseOrkestratorJson(now))

        every {
            seksjonRepository.hentSeksjonsvarEllerKastException(
                any(),
                any(),
                "verneplikt",
            )
        } returns objectMapper.readTree(avtjentVernepliktOrkestratorJson("ja"))
    }

    @Test
    fun `vi kan motta opplysningsbehov`() {
        testRapid.sendTestMessage(soknadsdataJson())
    }
}

private fun soknadsdataJson(): String {
    //language=JSON
    return """
        {"@event_name":"behov","@behovId":"6239be0b-3dec-4312-9df2-7e8e9759778a","@behov":["Søknadsdata"],"journalpostId":"717573262","tilstand":"AvventerSøknadsdataType","Søknadsdata":{"ident":"17477146473","dokumentInfoId":"753098601"},"ident":"17477146473","dokumentInfoId":"753098601","@final":true,"@id":"3f30dd1b-e0fc-42d9-a861-443c14284523","@opprettet":"2026-02-04T12:20:48.649930247","system_read_count":0,"system_participating_services":[{"id":"3f30dd1b-e0fc-42d9-a861-443c14284523","time":"2026-02-04T12:20:48.649930247","service":"dp-mottak","instance":"dp-mottak-666758cb44-d9kqp","image":"europe-north1-docker.pkg.dev/nais-management-233d/teamdagpenger/dp-mottak:2026.02.04-10.58-0dc8181"}]}
        """.trimIndent()
}
