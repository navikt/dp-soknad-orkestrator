package no.nav.dagpenger.soknad.orkestrator.søknad

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.equality.shouldBeEqualToComparingFields
import io.kotest.matchers.shouldBe
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import no.nav.dagpenger.soknad.orkestrator.api.models.SeksjonsnavnDTO
import no.nav.dagpenger.soknad.orkestrator.config.apiKonfigurasjon
import no.nav.dagpenger.soknad.orkestrator.config.objectMapper
import no.nav.dagpenger.soknad.orkestrator.db.Postgres.dataSource
import no.nav.dagpenger.soknad.orkestrator.db.Postgres.withMigratedDb
import no.nav.dagpenger.soknad.orkestrator.opplysning.BooleanSvar
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysningsbehov
import no.nav.dagpenger.soknad.orkestrator.opplysning.Opplysningstype
import no.nav.dagpenger.soknad.orkestrator.opplysning.Svar
import no.nav.dagpenger.soknad.orkestrator.opplysning.db.OpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.opplysning.db.OpplysningTabell
import no.nav.dagpenger.soknad.orkestrator.opplysning.db.SeksjonTabell
import no.nav.dagpenger.soknad.orkestrator.opplysning.seksjoner.Seksjon
import no.nav.dagpenger.soknad.orkestrator.opplysning.seksjoner.Seksjonsnavn
import no.nav.dagpenger.soknad.orkestrator.opplysning.seksjoner.getSeksjon
import no.nav.dagpenger.soknad.orkestrator.quizOpplysning.db.QuizOpplysningRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadRepository
import no.nav.dagpenger.soknad.orkestrator.søknad.db.SøknadTabell
import no.nav.dagpenger.soknad.orkestrator.utils.TestApplication
import no.nav.dagpenger.soknad.orkestrator.utils.TestApplication.autentisert
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test

class SøknadIntegrasjonstest {
    val søknadEndepunkt = "/soknad"

    lateinit var søknadRepository: SøknadRepository
    lateinit var opplysningRepository: OpplysningRepository
    lateinit var søknadService: SøknadService

    private val seksjonPath = "no.nav.dagpenger.soknad.orkestrator.opplysning.seksjoner.SeksjonKt"

    @BeforeTest
    fun setup() {
        mockkStatic(seksjonPath)
        every { getSeksjon(any()) } returns TestSeksjon

        withMigratedDb {
            søknadRepository = SøknadRepository(dataSource, mockk<QuizOpplysningRepository>(relaxed = true))
            opplysningRepository = OpplysningRepository(dataSource)
        }

        søknadService =
            SøknadService(
                søknadRepository = søknadRepository,
                opplysningRepository = opplysningRepository,
            ).also {
                it.setRapidsConnection(TestRapid())
            }
    }

    @Test
    fun `Ny søknad fører til opprettelse av søknad, opplysning og seksjon`() {
        withSøknadApi {
            autentisert(
                endepunkt = "$søknadEndepunkt/start",
                httpMethod = HttpMethod.Post,
            ).let { respons ->
                respons.status shouldBe HttpStatusCode.OK
                val søknadId = objectMapper.readValue(respons.bodyAsText(), UUID::class.java)
                søknadRepository.hent(søknadId)?.søknadId shouldBe søknadId
                søknadRepository.hent(søknadId)?.tilstand shouldBe Tilstand.PÅBEGYNT
                opplysningRepository.hentAlle(søknadId).first().also { opplysning ->
                    opplysning.seksjonsnavn shouldBe TestSeksjon.navn
                    opplysning.opplysningsbehovId shouldBe TestSeksjon.førsteOpplysningsbehov().id
                }
            }
        }
    }

    @Test
    fun `Det er mulig å hente neste seksjon når det finnes en søknad, seksjon og opplysning`() {
        val søknad = Søknad(UUID.randomUUID(), "12345678901")
        lagreSøknadSeksjonOpplysning(søknad)

        withSøknadApi {
            autentisert(
                endepunkt = "$søknadEndepunkt/${søknad.søknadId}/neste",
                httpMethod = HttpMethod.Get,
            ).let { respons ->
                respons.status shouldBe HttpStatusCode.OK
                val seksjoner = objectMapper.readValue<OrkestratorSoknadDTO>(respons.bodyAsText())
                seksjoner.seksjoner.first().navn shouldBe SeksjonsnavnDTO.bostedsland
                seksjoner.seksjoner.first().besvarteOpplysninger shouldBe emptyList()
                seksjoner.seksjoner
                    .first()
                    .nesteUbesvarteOpplysning!!
                    .tekstnøkkel shouldBe TestSeksjon.opplysningsbehov1.tekstnøkkel
                seksjoner.seksjoner.first().erFullført shouldBe false
            }
        }
    }

    @Test
    fun `Det er mulig å besvare neste ubesvarte opplysning`() {
        val søknad = Søknad(UUID.randomUUID(), "12345678901")
        lagreSøknadSeksjonOpplysning(søknad)

        val nesteOpplysning = opplysningRepository.hentAlle(søknad.søknadId).first()

        val svar =
            BooleanSvar(
                opplysningId = nesteOpplysning.opplysningId,
                verdi = true,
            )

        withSøknadApi {
            autentisert(
                endepunkt = "$søknadEndepunkt/${søknad.søknadId}/svar",
                httpMethod = HttpMethod.Put,
                body = objectMapper.writeValueAsString(svar),
            ).let { respons ->
                respons.status shouldBe HttpStatusCode.OK
                opplysningRepository.hent(nesteOpplysning.opplysningId)?.svar?.shouldBeEqualToComparingFields(svar)
            }
        }
    }

    private fun lagreSøknadSeksjonOpplysning(søknad: Søknad) {
        transaction {
            val søknadDBId =
                SøknadTabell
                    .insertAndGetId {
                        it[søknadId] = søknad.søknadId
                        it[ident] = søknad.ident
                        it[tilstand] = søknad.tilstand.name
                    }.value

            val seksjonDBId =
                SeksjonTabell
                    .insertAndGetId {
                        it[navn] = TestSeksjon.navn.name
                        it[versjon] = TestSeksjon.versjon
                        it[søknadId] = søknadDBId
                    }.value

            OpplysningTabell.insert {
                it[opplysningId] = UUID.randomUUID()
                it[seksjonId] = seksjonDBId
                it[opplysningsbehovId] = TestSeksjon.opplysningsbehov1.id
                it[type] = TestSeksjon.opplysningsbehov1.type.name
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

object TestSeksjon : Seksjon() {
    override val navn = Seksjonsnavn.BOSTEDSLAND
    override val versjon = "TESTSEKSJON_V1"

    val opplysningsbehov1 = Opplysningsbehov(1, "tekstnøkkel1", Opplysningstype.BOOLEAN)
    val opplysningsbehov2 =
        Opplysningsbehov(2, "tekstnøkkel2", Opplysningstype.LAND, listOf("NOR", "SWE", "FIN"))
    val opplysningsbehov3 = Opplysningsbehov(3, "tekstnøkkel3", Opplysningstype.TEKST)
    val opplysningsbehov4 = Opplysningsbehov(4, "tekstnøkkel4", Opplysningstype.TEKST)

    override fun førsteOpplysningsbehov() = opplysningsbehov1

    override fun nesteOpplysningsbehov(
        svar: Svar<*>,
        opplysningsbehovId: Int,
    ): Opplysningsbehov? =
        when (opplysningsbehovId) {
            opplysningsbehov1.id -> if (svar.verdi == true) opplysningsbehov2 else opplysningsbehov3
            opplysningsbehov2.id -> opplysningsbehov3
            opplysningsbehov3.id -> opplysningsbehov4
            opplysningsbehov4.id -> null
            else -> null
        }

    override fun getOpplysningsbehov(opplysningsbehovId: Int): Opplysningsbehov =
        when (opplysningsbehovId) {
            opplysningsbehov1.id -> opplysningsbehov1
            opplysningsbehov2.id -> opplysningsbehov2
            opplysningsbehov3.id -> opplysningsbehov3
            opplysningsbehov4.id -> opplysningsbehov4
            else -> throw IllegalArgumentException("Ukjent opplysning med id: $opplysningsbehovId")
        }

    override fun avhengigheter(opplysningsbehovId: Int): List<Int> =
        when (opplysningsbehovId) {
            opplysningsbehov1.id -> listOf(opplysningsbehov2.id)
            else -> emptyList()
        }

    override fun validerSvar(
        opplysningsbehovId: Int,
        svar: Svar<*>,
    ) {
        if (opplysningsbehovId == opplysningsbehov2.id) {
            if (opplysningsbehov2.gyldigeSvar?.contains(svar.verdi) != true) {
                throw IllegalArgumentException("$svar er ikke et gyldig svar")
            }
        }
    }
}
